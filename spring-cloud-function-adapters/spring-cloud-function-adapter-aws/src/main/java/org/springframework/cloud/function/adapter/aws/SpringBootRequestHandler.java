/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.function.adapter.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.cloud.function.context.AbstractSpringFunctionAdapterInitializer;

/**
 * @param <E> event type
 * @param <O> result types
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class SpringBootRequestHandler<E, O> extends AbstractSpringFunctionAdapterInitializer<Context>
		implements RequestHandler<E, Object> {

	public SpringBootRequestHandler(Class<?> configurationClass) {
		super(configurationClass);
	}

	public SpringBootRequestHandler() {
		super();
	}

	@Override
	public Object handleRequest(E event, Context context) {
		initialize(context);
		Object input = acceptsInput() ? convertEvent(event) : "";
		Publisher<?> output = apply(extract(input));
		return result(input, output);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T result(Object input, Publisher<?> output) {
		List<O> result = new ArrayList<>();
		for (Object value : Flux.from(output).toIterable()) {
			result.add(convertOutput(value));
		}
		if (isSingleValue(input) && result.size() == 1) {
			return (T) result.get(0);
		}
		return (T) result;
	}

	protected boolean acceptsInput() {
		return !this.getInspector().getInputType(function()).equals(Void.class);
	}

	protected boolean returnsOutput() {
		return !this.getInspector().getOutputType(function()).equals(Void.class);
	}

	private boolean isSingleValue(Object input) {
		return !(input instanceof Collection);
	}

	private Flux<?> extract(Object input) {
		if (input instanceof Collection) {
			return Flux.fromIterable((Iterable<?>) input);
		}
		return Flux.just(input);
	}

	protected Object convertEvent(E event) {
		return event;
	}

	@SuppressWarnings("unchecked")
	protected O convertOutput(Object output) {
		return (O) output;
	}

}
