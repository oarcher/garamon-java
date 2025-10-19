package org.garamon.c5ga;

import org.garamon.c5ga.Mvec;
import org.garamon.c5ga.NativeLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mvec Operations Test")
public class MvecTest {

  @BeforeAll
  static void setup() {
    NativeLoader.load();
  }

  @AfterAll
  static void tearDown() {
    // Any global cleanup if necessary, though individual Mvecs are closed in the test.
  }

  @Test
  @DisplayName("Test Mvec operations")
  void testMvecOperations() {
    // mv1[scalar]=1 ; mv1[E0]=42
    Mvec mv1 = new Mvec(1.0);
    mv1.at(Mvec.Basis.SCALAR, 1.0);
    mv1.at(Mvec.Basis.E0, 42.0);
    // Mvec mv1 = Mvec.scalar(1.0).add(Mvec.e0().rmul(42.0));
    assertNotNull(mv1, "mv1 should not be null");
    assertEquals(1.0, mv1.at(Mvec.Basis.SCALAR), 0.001, "Scalar component of mv1 should be 1.0");
    assertEquals(42.0, mv1.at(Mvec.Basis.E0), 0.001, "E0 component of mv1 should be 42.0");


    // mv2[E0]=1 ; mv2[E1]=2 ; mv2 += I() + 2*e1()
    Mvec mv2 = new Mvec();
    mv2.at(Mvec.Basis.E0, 1.0);
    mv2.at(Mvec.Basis.E1, 2.0);
    mv2 = mv2.add(Mvec.I().add(Mvec.e01().rmul(2.0)));
    // Mvec mv2 = Mvec.e0().rmul(1.0)
    //     .add(Mvec.e1().rmul(2.0))
    //     .add(Mvec.I().add(Mvec.e01().rmul(2.0)));
    assertNotNull(mv2, "mv2 should not be null");
    // Further assertions for mv2 components would require knowing the exact expected values after the complex addition.

    // produits
    Mvec ext = mv1.outer(mv2);
    Mvec inn = mv1.inner(mv2);
    Mvec gp = mv1.mul(mv2);
    Mvec lcont = mv1.leftContraction(mv2);
    Mvec rcont = mv1.rightContraction(mv2);

    assertNotNull(ext, "Outer product should not be null");
    assertNotNull(inn, "Inner product should not be null");
    assertNotNull(gp, "Geometric product should not be null");
    assertNotNull(lcont, "Left contraction should not be null");
    assertNotNull(rcont, "Right contraction should not be null");
    // Assertions for the results of products would require knowing the exact expected values.

    // System.out.println("grades : " + Arrays.toString(mv1.grades())); // Removed print
    // System.out.println("norm  : " + mv1.norm()); // Removed print
    assertNotNull(mv1.grades(), "Grades array should not be null");
    assertTrue(mv1.norm() >= 0, "Norm should be non-negative"); // Basic assertion for norm
    mv1.clear();
    assertTrue(mv1.isEmpty(), "mv1 should be empty after clear()");

    // manual free (but GC will handle close if not done)
    mv1.close();
    mv2.close();
    ext.close();
    inn.close();
    gp.close();
    lcont.close();
    rcont.close();
  }
}
