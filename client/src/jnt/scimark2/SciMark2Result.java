package jnt.scimark2;

import java.io.Serializable;

public class SciMark2Result implements Serializable {

  private final double[] result;
  private final int FFT_size, SOR_size, Sparse_size_M, Sparse_size_nz, LU_size;

  public SciMark2Result(double[] result,
                        int FFT_size,
                        int SOR_size,
                        int Sparse_size_M,
                        int Sparse_size_nz,
                        int LU_size) throws IllegalArgumentException {
    if (result.length != 6) {
      throw new IllegalArgumentException("Reult array size must be 6!");
    }
    this.result = result;
    this.FFT_size = FFT_size;
    this.SOR_size = SOR_size;
    this.Sparse_size_M = Sparse_size_M;
    this.Sparse_size_nz = Sparse_size_nz;
    this.LU_size = LU_size;
  }

  public String toString() {
    // print out results
    StringBuffer buffer = new StringBuffer("");

    buffer.append("SciMark 2.0a" + "\n");
    buffer.append("\n");
    buffer.append("Composite Score: " + result[0] + "\n");
    buffer.append("FFT ("+FFT_size+"): ");
    if (result[1]==0.0)
        buffer.append(" ERROR, INVALID NUMERICAL RESULT!" + "\n");
    else
        buffer.append(result[1] + "\n");

    buffer.append("SOR ("+SOR_size+"x"+ SOR_size+"): "
            + "  " + result[2] + "\n");
    buffer.append("Monte Carlo : " + result[3] + "\n");
    buffer.append("Sparse matmult (N="+ Sparse_size_M+
            ", nz=" + Sparse_size_nz + "): " + result[4] + "\n");
    buffer.append("LU (" + LU_size + "x" + LU_size + "): ");
    if (result[5]==0.0)
        buffer.append(" ERROR, INVALID NUMERICAL RESULT!" + "\n");
    else
        buffer.append(result[5] + "\n");

    return buffer.toString();
  }
  
  public double getComposite() {
      return result[0];
  }
}
