package jnt.scimark2;

import jgrid.comp.compute.Task;
import jgrid.comp.compute.HostContext;
import java.io.Serializable;

/**
	SciMark2: A Java numerical benchmark measuring performance
	of computational kernels for FFTs, Monte Carlo simulation,
	sparse matrix computations, Jacobi SOR, and dense LU matrix
	factorizations.


*/

/**
*  Benchmark 5 kernels with individual Mflops.
*  "results[0]" has the average Mflop rate.
*/
public class SciMark2Task implements Task
{
  private double resolution;
  private boolean large;

  public SciMark2Task(double resolution, boolean large) {
    this.resolution = resolution;
    this.large = large;
  }

  public SciMark2Task(boolean large) {
    this.resolution = 0.0;
    this.large = large;
  }

	public Serializable execute()
	{
		// default to the (small) cache-contained version
        Constants constants = new Constants();
        Kernel kernel = new Kernel();

		double min_time = constants.RESOLUTION_DEFAULT;
        if (resolution > 1.0) {
          min_time = resolution;
        }

		int FFT_size = constants.FFT_SIZE;
		int SOR_size =  constants.SOR_SIZE;
		int Sparse_size_M = constants.SPARSE_SIZE_M;
		int Sparse_size_nz = constants.SPARSE_SIZE_nz;
		int LU_size = constants.LU_SIZE;

		// look for runtime options

        if (large) {
          FFT_size = constants.LG_FFT_SIZE;
          SOR_size = constants.LG_SOR_SIZE;
          Sparse_size_M = constants.LG_SPARSE_SIZE_M;
          Sparse_size_nz = constants.LG_SPARSE_SIZE_nz;
          LU_size = constants.LG_LU_SIZE;
        }

		// run the benchmark

		double res[] = new double[6];
		Random R = new Random(constants.RANDOM_SEED);

		res[1] = kernel.measureFFT( FFT_size, min_time, R);
		res[2] = kernel.measureSOR( SOR_size, min_time, R);
		res[3] = kernel.measureMonteCarlo(min_time, R);
		res[4] = kernel.measureSparseMatmult( Sparse_size_M,
					Sparse_size_nz, min_time, R);
		res[5] = kernel.measureLU( LU_size, min_time, R);


		res[0] = (res[1] + res[2] + res[3] + res[4] + res[5]) / 5;

        return new SciMark2Result(res, FFT_size, SOR_size, Sparse_size_M, Sparse_size_nz, LU_size);
	}

  /**
   * setHostContext
   *
   * @param hostContext HostContext
   */
  public void setHostContext(HostContext hostContext) {
  }

  /**
   * cancel
   */
  public void cancel() {
  }

  /**
   * resume
   */
  public void resume() {
  }

  /**
   * suspend
   */
  public void suspend() {
  }

}
