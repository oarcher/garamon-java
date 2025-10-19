import org.garamon.c5ga.Mvec;
import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    // mv1[scalar]=1 ; mv1[E0]=42
    Mvec mv1 = new Mvec(1.0);
    mv1.at(Mvec.Basis.SCALAR, 1.0);

    mv1.at(Mvec.Basis.E0, 42.0);
    // Mvec mv1 =
    // Mvec.scalar(1.0).add(Mvec.e0().rmul(42.0));
    System.out.print("mv1 : ");
    mv1.display();

    // mv2[E0]=1 ; mv2[E1]=2 ;
    // mv2 += I() + 2*e1()
    Mvec mv2 = new Mvec();
    mv2.at(Mvec.Basis.E0, 1.0);
    mv2.at(Mvec.Basis.E1, 2.0);
    mv2 = mv2.add(Mvec.I().add(Mvec.e01().rmul(2.0)));
    // Mvec mv2 = Mvec.e0().rmul(1.0)
    // .add(Mvec.e1().rmul(2.0))
    // .add(Mvec.I().add(Mvec.e01().rmul(2.0)));
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
