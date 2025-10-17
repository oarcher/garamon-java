package org.garamon.project_namespace;
import org.garamon.project_namespace.Mvec;
import org.garamon.project_namespace.NativeLoader;
import java.util.Arrays;

public class Sample {

  static {
    NativeLoader.load();
  }
  public static void main(String[] args) {


    // mv1[scalar]=1 ; mv1[Eproject_first_vector_basis]=42
    Mvec mv1 = new Mvec(1.0);
    mv1.at(Mvec.Basis.SCALAR, 1.0);


    mv1.at(Mvec.Basis.Eproject_first_vector_basis, 42.0);
    // Mvec mv1 = Mvec.scalar(1.0).add(Mvec.eproject_first_vector_basis().rmul(42.0));
    System.out.print("mv1 : ");
    mv1.display();

    // mv2[Eproject_first_vector_basis]=1 ; mv2[Eproject_second_vector_basis]=2 ; mv2 += I() + 2*eproject_second_vector_basis()
    Mvec mv2 = new Mvec();
    mv2.at(Mvec.Basis.Eproject_first_vector_basis, 1.0);
    mv2.at(Mvec.Basis.Eproject_second_vector_basis, 2.0);
    mv2 = mv2.add(Mvec.I().add(Mvec.eproject_first_vector_basisproject_second_vector_basis().rmul(2.0)));
    // Mvec mv2 = Mvec.eproject_first_vector_basis().rmul(1.0)
    //     .add(Mvec.eproject_second_vector_basis().rmul(2.0))
    //     .add(Mvec.I().add(Mvec.eproject_first_vector_basisproject_second_vector_basis().rmul(2.0)));
    System.out.print("mv2 : ");
    mv2.display();
    System.out.println();

    // produits
    Mvec ext = mv1.outer(mv2);
    Mvec inn = mv1.inner(mv2);
    Mvec gp = mv1.mul(mv2);
    Mvec lcont = mv1.leftContraction(mv2);
    Mvec rcont = mv1.rightContraction(mv2);

    System.out.print("outer product     : ");
    ext.display();
    System.out.print("inner product     : ");
    inn.display();
    System.out.print("geometric product : ");
    gp.display();
    System.out.print("left contraction  : ");
    lcont.display();
    System.out.print("right contraction : ");
    rcont.display();
    System.out.println();

    System.out.println("grades : " + Arrays.toString(mv1.grades()));
    System.out.println("norm  : " + mv1.norm());
    mv1.clear();
    if (mv1.isEmpty())
      System.out.println("mv1 is empty: ok");

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
