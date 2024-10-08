package com.example.webtorest.restclient.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConcurrentSupports {

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

	public static <T, R, RESULT> RESULT invokeAndMap(
		Supplier<T> requestSupplier1,
		Supplier<R> requestSupplier2,
		BiFunction<T, R, RESULT> mapper
	) {
		Pair<T, R> pair = invoke(requestSupplier1, requestSupplier2);

		return mapper.apply(
			pair.getLeft(),
			pair.getRight()
		);
	}

	public static <T, R> Pair<T, R> invoke(
		Supplier<T> requestSupplier1,
		Supplier<R> requestSupplier2
	) {
		CompletableFuture<T> future1 = CompletableFuture.supplyAsync(requestSupplier1, EXECUTOR_SERVICE);
		CompletableFuture<R> future2 = CompletableFuture.supplyAsync(requestSupplier2, EXECUTOR_SERVICE);

		CompletableFuture.allOf(future1, future2).join();

		return Pair.of(
			future1.join(),
			future2.join()
		);
	}
}
