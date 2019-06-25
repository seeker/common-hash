/*
 * The MIT License (MIT)
 * Copyright (c) 2019 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.seeker.commonhash.perceptual.phash.kernel;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.device.Device;
import com.aparapi.internal.kernel.KernelManager;
import com.google.common.primitives.Doubles;

public class DCTKernel extends Kernel {
	public static final int DEFAULT_MATRIX_SIZE = 8;
	private final int N; // matrix size
	private final int matrixArea;
	private final double[] dctCoefficients;
	private final double[] matrix;
	private final double[] result;

	private Device device;
	private Range range;

	/**
	 * Create a new DCT kernel for a 8x8 matrix;
	 */
	public DCTKernel() {
		this(DEFAULT_MATRIX_SIZE);
	}

	/**
	 * Create a new DCT kernel for the given size.
	 * 
	 * @param matrixSize
	 *            size of the matrix that this kernel will opoerate on
	 */
	public DCTKernel(int matrixSize) {
		this.N = matrixSize;
		this.matrixArea = N*N;
		this.matrix = new double[matrixArea];
		this.result = new double[matrixArea];
		this.dctCoefficients = new double[N];
		initCoefficients();

		setDevice(KernelManager.instance().bestDevice());
	}

	public void setDevice(Device device) {
		range = Range.create2D(device, N, N);
	}

	private void initCoefficients() {
		for (int i = 1; i < N; i++) {
			dctCoefficients[i] = 1;
		}

		dctCoefficients[0] = 1 / Math.sqrt(2.0);
	}

	@Override
	public void run() {
		int u = getGlobalId(0);
		int v = getGlobalId(1);

		double sum = 0.0;

		for (int g = 0; g < matrixArea; g++) {
			sum += cos(((2 * (g / N) + 1) / (2.0 * N)) * u * Math.PI)
					* cos(((2 * (g % N) + 1) / (2.0 * N)) * v * Math.PI) * (matrix[g]);
		}

		sum *= ((dctCoefficients[u] * dctCoefficients[v]) / 4.0);

		result[u * N + v] = sum;
	}

	/**
	 * 
	 * @param matrix
	 * @return
	 * 
	 * @see DCT function from http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java
	 */
	public synchronized double[] transformDCT(double[] matrix) {
		for (int i = 0; i < matrixArea; i++) {
			this.matrix[i] = matrix[i];
		}

		execute(range);

		return Doubles.concat(result);
	}
}
