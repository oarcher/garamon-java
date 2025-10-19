package org.garamon.c5ga;

import java.lang.ref.Cleaner;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import org.garamon.c5ga.Mvec_h;

/**
 * Class defining multivectors.
 *
 * <p>This class provides a Java wrapper for the C++ Mvec class, allowing for
 * operations on multivectors in Geometric Algebra.</p>
 */
public final class Mvec implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();


    private static final class Native implements Runnable {
        MemorySegment seg;

        Native(MemorySegment s) {
            this.seg = s;
        }

        @Override
        public void run() {
            if (seg != null)
                Mvec_h.Mvec_delete(seg);
        }
    }

    private final Native nativeState;
    private final Cleaner.Cleanable cleanable;

    private MemorySegment seg() {
        return nativeState.seg;
    }

    private Mvec(MemorySegment seg) {
        this.nativeState = new Native(seg);
        this.cleanable = CLEANER.register(this, nativeState);
    }

    /**
     * Default constructor, generates an empty multivector equivalent to the scalar 0.
     */
    public Mvec() {
        this(Mvec_h.Mvec_new_empty());
    }

    /**
     * Constructor of Mvec from a scalar.
     * @param v The scalar value.
     */
    public Mvec(double v) {
        this(Mvec_h.Mvec_new_scalar(v));
    }

    /**
     * Constructor of Mvec from a basis vector and a value.
     * @param basisIndex The basis vector index related to the query.
     * @param value The coefficient of the multivector corresponding to the "basisIndex" component.
     */
    public Mvec(int basisIndex, double value) {
        this(Mvec_h.Mvec_new_empty());
        Mvec_h.Mvec_set_coeff(seg(), basisIndex, value);
    }
    

/**
 * @return a multivector that contains only the unit basis k-vector 0.
 */
public static Mvec e0(){
    return new Mvec(Mvec_h.Mvec_e0());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1.
 */
public static Mvec e1(){
    return new Mvec(Mvec_h.Mvec_e1());
}
/**
 * @return a multivector that contains only the unit basis k-vector 2.
 */
public static Mvec e2(){
    return new Mvec(Mvec_h.Mvec_e2());
}
/**
 * @return a multivector that contains only the unit basis k-vector 3.
 */
public static Mvec e3(){
    return new Mvec(Mvec_h.Mvec_e3());
}
/**
 * @return a multivector that contains only the unit basis k-vector 4.
 */
public static Mvec e4(){
    return new Mvec(Mvec_h.Mvec_e4());
}
/**
 * @return a multivector that contains only the unit basis k-vector 5.
 */
public static Mvec e5(){
    return new Mvec(Mvec_h.Mvec_e5());
}
/**
 * @return a multivector that contains only the unit basis k-vector i.
 */
public static Mvec ei(){
    return new Mvec(Mvec_h.Mvec_ei());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01.
 */
public static Mvec e01(){
    return new Mvec(Mvec_h.Mvec_e01());
}
/**
 * @return a multivector that contains only the unit basis k-vector 02.
 */
public static Mvec e02(){
    return new Mvec(Mvec_h.Mvec_e02());
}
/**
 * @return a multivector that contains only the unit basis k-vector 03.
 */
public static Mvec e03(){
    return new Mvec(Mvec_h.Mvec_e03());
}
/**
 * @return a multivector that contains only the unit basis k-vector 04.
 */
public static Mvec e04(){
    return new Mvec(Mvec_h.Mvec_e04());
}
/**
 * @return a multivector that contains only the unit basis k-vector 05.
 */
public static Mvec e05(){
    return new Mvec(Mvec_h.Mvec_e05());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0i.
 */
public static Mvec e0i(){
    return new Mvec(Mvec_h.Mvec_e0i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 12.
 */
public static Mvec e12(){
    return new Mvec(Mvec_h.Mvec_e12());
}
/**
 * @return a multivector that contains only the unit basis k-vector 13.
 */
public static Mvec e13(){
    return new Mvec(Mvec_h.Mvec_e13());
}
/**
 * @return a multivector that contains only the unit basis k-vector 14.
 */
public static Mvec e14(){
    return new Mvec(Mvec_h.Mvec_e14());
}
/**
 * @return a multivector that contains only the unit basis k-vector 15.
 */
public static Mvec e15(){
    return new Mvec(Mvec_h.Mvec_e15());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1i.
 */
public static Mvec e1i(){
    return new Mvec(Mvec_h.Mvec_e1i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 23.
 */
public static Mvec e23(){
    return new Mvec(Mvec_h.Mvec_e23());
}
/**
 * @return a multivector that contains only the unit basis k-vector 24.
 */
public static Mvec e24(){
    return new Mvec(Mvec_h.Mvec_e24());
}
/**
 * @return a multivector that contains only the unit basis k-vector 25.
 */
public static Mvec e25(){
    return new Mvec(Mvec_h.Mvec_e25());
}
/**
 * @return a multivector that contains only the unit basis k-vector 2i.
 */
public static Mvec e2i(){
    return new Mvec(Mvec_h.Mvec_e2i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 34.
 */
public static Mvec e34(){
    return new Mvec(Mvec_h.Mvec_e34());
}
/**
 * @return a multivector that contains only the unit basis k-vector 35.
 */
public static Mvec e35(){
    return new Mvec(Mvec_h.Mvec_e35());
}
/**
 * @return a multivector that contains only the unit basis k-vector 3i.
 */
public static Mvec e3i(){
    return new Mvec(Mvec_h.Mvec_e3i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 45.
 */
public static Mvec e45(){
    return new Mvec(Mvec_h.Mvec_e45());
}
/**
 * @return a multivector that contains only the unit basis k-vector 4i.
 */
public static Mvec e4i(){
    return new Mvec(Mvec_h.Mvec_e4i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 5i.
 */
public static Mvec e5i(){
    return new Mvec(Mvec_h.Mvec_e5i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 012.
 */
public static Mvec e012(){
    return new Mvec(Mvec_h.Mvec_e012());
}
/**
 * @return a multivector that contains only the unit basis k-vector 013.
 */
public static Mvec e013(){
    return new Mvec(Mvec_h.Mvec_e013());
}
/**
 * @return a multivector that contains only the unit basis k-vector 014.
 */
public static Mvec e014(){
    return new Mvec(Mvec_h.Mvec_e014());
}
/**
 * @return a multivector that contains only the unit basis k-vector 015.
 */
public static Mvec e015(){
    return new Mvec(Mvec_h.Mvec_e015());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01i.
 */
public static Mvec e01i(){
    return new Mvec(Mvec_h.Mvec_e01i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 023.
 */
public static Mvec e023(){
    return new Mvec(Mvec_h.Mvec_e023());
}
/**
 * @return a multivector that contains only the unit basis k-vector 024.
 */
public static Mvec e024(){
    return new Mvec(Mvec_h.Mvec_e024());
}
/**
 * @return a multivector that contains only the unit basis k-vector 025.
 */
public static Mvec e025(){
    return new Mvec(Mvec_h.Mvec_e025());
}
/**
 * @return a multivector that contains only the unit basis k-vector 02i.
 */
public static Mvec e02i(){
    return new Mvec(Mvec_h.Mvec_e02i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 034.
 */
public static Mvec e034(){
    return new Mvec(Mvec_h.Mvec_e034());
}
/**
 * @return a multivector that contains only the unit basis k-vector 035.
 */
public static Mvec e035(){
    return new Mvec(Mvec_h.Mvec_e035());
}
/**
 * @return a multivector that contains only the unit basis k-vector 03i.
 */
public static Mvec e03i(){
    return new Mvec(Mvec_h.Mvec_e03i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 045.
 */
public static Mvec e045(){
    return new Mvec(Mvec_h.Mvec_e045());
}
/**
 * @return a multivector that contains only the unit basis k-vector 04i.
 */
public static Mvec e04i(){
    return new Mvec(Mvec_h.Mvec_e04i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 05i.
 */
public static Mvec e05i(){
    return new Mvec(Mvec_h.Mvec_e05i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 123.
 */
public static Mvec e123(){
    return new Mvec(Mvec_h.Mvec_e123());
}
/**
 * @return a multivector that contains only the unit basis k-vector 124.
 */
public static Mvec e124(){
    return new Mvec(Mvec_h.Mvec_e124());
}
/**
 * @return a multivector that contains only the unit basis k-vector 125.
 */
public static Mvec e125(){
    return new Mvec(Mvec_h.Mvec_e125());
}
/**
 * @return a multivector that contains only the unit basis k-vector 12i.
 */
public static Mvec e12i(){
    return new Mvec(Mvec_h.Mvec_e12i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 134.
 */
public static Mvec e134(){
    return new Mvec(Mvec_h.Mvec_e134());
}
/**
 * @return a multivector that contains only the unit basis k-vector 135.
 */
public static Mvec e135(){
    return new Mvec(Mvec_h.Mvec_e135());
}
/**
 * @return a multivector that contains only the unit basis k-vector 13i.
 */
public static Mvec e13i(){
    return new Mvec(Mvec_h.Mvec_e13i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 145.
 */
public static Mvec e145(){
    return new Mvec(Mvec_h.Mvec_e145());
}
/**
 * @return a multivector that contains only the unit basis k-vector 14i.
 */
public static Mvec e14i(){
    return new Mvec(Mvec_h.Mvec_e14i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 15i.
 */
public static Mvec e15i(){
    return new Mvec(Mvec_h.Mvec_e15i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 234.
 */
public static Mvec e234(){
    return new Mvec(Mvec_h.Mvec_e234());
}
/**
 * @return a multivector that contains only the unit basis k-vector 235.
 */
public static Mvec e235(){
    return new Mvec(Mvec_h.Mvec_e235());
}
/**
 * @return a multivector that contains only the unit basis k-vector 23i.
 */
public static Mvec e23i(){
    return new Mvec(Mvec_h.Mvec_e23i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 245.
 */
public static Mvec e245(){
    return new Mvec(Mvec_h.Mvec_e245());
}
/**
 * @return a multivector that contains only the unit basis k-vector 24i.
 */
public static Mvec e24i(){
    return new Mvec(Mvec_h.Mvec_e24i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 25i.
 */
public static Mvec e25i(){
    return new Mvec(Mvec_h.Mvec_e25i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 345.
 */
public static Mvec e345(){
    return new Mvec(Mvec_h.Mvec_e345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 34i.
 */
public static Mvec e34i(){
    return new Mvec(Mvec_h.Mvec_e34i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 35i.
 */
public static Mvec e35i(){
    return new Mvec(Mvec_h.Mvec_e35i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 45i.
 */
public static Mvec e45i(){
    return new Mvec(Mvec_h.Mvec_e45i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0123.
 */
public static Mvec e0123(){
    return new Mvec(Mvec_h.Mvec_e0123());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0124.
 */
public static Mvec e0124(){
    return new Mvec(Mvec_h.Mvec_e0124());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0125.
 */
public static Mvec e0125(){
    return new Mvec(Mvec_h.Mvec_e0125());
}
/**
 * @return a multivector that contains only the unit basis k-vector 012i.
 */
public static Mvec e012i(){
    return new Mvec(Mvec_h.Mvec_e012i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0134.
 */
public static Mvec e0134(){
    return new Mvec(Mvec_h.Mvec_e0134());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0135.
 */
public static Mvec e0135(){
    return new Mvec(Mvec_h.Mvec_e0135());
}
/**
 * @return a multivector that contains only the unit basis k-vector 013i.
 */
public static Mvec e013i(){
    return new Mvec(Mvec_h.Mvec_e013i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0145.
 */
public static Mvec e0145(){
    return new Mvec(Mvec_h.Mvec_e0145());
}
/**
 * @return a multivector that contains only the unit basis k-vector 014i.
 */
public static Mvec e014i(){
    return new Mvec(Mvec_h.Mvec_e014i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 015i.
 */
public static Mvec e015i(){
    return new Mvec(Mvec_h.Mvec_e015i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0234.
 */
public static Mvec e0234(){
    return new Mvec(Mvec_h.Mvec_e0234());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0235.
 */
public static Mvec e0235(){
    return new Mvec(Mvec_h.Mvec_e0235());
}
/**
 * @return a multivector that contains only the unit basis k-vector 023i.
 */
public static Mvec e023i(){
    return new Mvec(Mvec_h.Mvec_e023i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0245.
 */
public static Mvec e0245(){
    return new Mvec(Mvec_h.Mvec_e0245());
}
/**
 * @return a multivector that contains only the unit basis k-vector 024i.
 */
public static Mvec e024i(){
    return new Mvec(Mvec_h.Mvec_e024i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 025i.
 */
public static Mvec e025i(){
    return new Mvec(Mvec_h.Mvec_e025i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0345.
 */
public static Mvec e0345(){
    return new Mvec(Mvec_h.Mvec_e0345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 034i.
 */
public static Mvec e034i(){
    return new Mvec(Mvec_h.Mvec_e034i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 035i.
 */
public static Mvec e035i(){
    return new Mvec(Mvec_h.Mvec_e035i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 045i.
 */
public static Mvec e045i(){
    return new Mvec(Mvec_h.Mvec_e045i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1234.
 */
public static Mvec e1234(){
    return new Mvec(Mvec_h.Mvec_e1234());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1235.
 */
public static Mvec e1235(){
    return new Mvec(Mvec_h.Mvec_e1235());
}
/**
 * @return a multivector that contains only the unit basis k-vector 123i.
 */
public static Mvec e123i(){
    return new Mvec(Mvec_h.Mvec_e123i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1245.
 */
public static Mvec e1245(){
    return new Mvec(Mvec_h.Mvec_e1245());
}
/**
 * @return a multivector that contains only the unit basis k-vector 124i.
 */
public static Mvec e124i(){
    return new Mvec(Mvec_h.Mvec_e124i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 125i.
 */
public static Mvec e125i(){
    return new Mvec(Mvec_h.Mvec_e125i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1345.
 */
public static Mvec e1345(){
    return new Mvec(Mvec_h.Mvec_e1345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 134i.
 */
public static Mvec e134i(){
    return new Mvec(Mvec_h.Mvec_e134i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 135i.
 */
public static Mvec e135i(){
    return new Mvec(Mvec_h.Mvec_e135i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 145i.
 */
public static Mvec e145i(){
    return new Mvec(Mvec_h.Mvec_e145i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 2345.
 */
public static Mvec e2345(){
    return new Mvec(Mvec_h.Mvec_e2345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 234i.
 */
public static Mvec e234i(){
    return new Mvec(Mvec_h.Mvec_e234i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 235i.
 */
public static Mvec e235i(){
    return new Mvec(Mvec_h.Mvec_e235i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 245i.
 */
public static Mvec e245i(){
    return new Mvec(Mvec_h.Mvec_e245i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 345i.
 */
public static Mvec e345i(){
    return new Mvec(Mvec_h.Mvec_e345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01234.
 */
public static Mvec e01234(){
    return new Mvec(Mvec_h.Mvec_e01234());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01235.
 */
public static Mvec e01235(){
    return new Mvec(Mvec_h.Mvec_e01235());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0123i.
 */
public static Mvec e0123i(){
    return new Mvec(Mvec_h.Mvec_e0123i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01245.
 */
public static Mvec e01245(){
    return new Mvec(Mvec_h.Mvec_e01245());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0124i.
 */
public static Mvec e0124i(){
    return new Mvec(Mvec_h.Mvec_e0124i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0125i.
 */
public static Mvec e0125i(){
    return new Mvec(Mvec_h.Mvec_e0125i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01345.
 */
public static Mvec e01345(){
    return new Mvec(Mvec_h.Mvec_e01345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0134i.
 */
public static Mvec e0134i(){
    return new Mvec(Mvec_h.Mvec_e0134i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0135i.
 */
public static Mvec e0135i(){
    return new Mvec(Mvec_h.Mvec_e0135i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0145i.
 */
public static Mvec e0145i(){
    return new Mvec(Mvec_h.Mvec_e0145i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 02345.
 */
public static Mvec e02345(){
    return new Mvec(Mvec_h.Mvec_e02345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0234i.
 */
public static Mvec e0234i(){
    return new Mvec(Mvec_h.Mvec_e0234i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0235i.
 */
public static Mvec e0235i(){
    return new Mvec(Mvec_h.Mvec_e0235i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0245i.
 */
public static Mvec e0245i(){
    return new Mvec(Mvec_h.Mvec_e0245i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 0345i.
 */
public static Mvec e0345i(){
    return new Mvec(Mvec_h.Mvec_e0345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 12345.
 */
public static Mvec e12345(){
    return new Mvec(Mvec_h.Mvec_e12345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1234i.
 */
public static Mvec e1234i(){
    return new Mvec(Mvec_h.Mvec_e1234i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1235i.
 */
public static Mvec e1235i(){
    return new Mvec(Mvec_h.Mvec_e1235i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1245i.
 */
public static Mvec e1245i(){
    return new Mvec(Mvec_h.Mvec_e1245i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 1345i.
 */
public static Mvec e1345i(){
    return new Mvec(Mvec_h.Mvec_e1345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 2345i.
 */
public static Mvec e2345i(){
    return new Mvec(Mvec_h.Mvec_e2345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 012345.
 */
public static Mvec e012345(){
    return new Mvec(Mvec_h.Mvec_e012345());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01234i.
 */
public static Mvec e01234i(){
    return new Mvec(Mvec_h.Mvec_e01234i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01235i.
 */
public static Mvec e01235i(){
    return new Mvec(Mvec_h.Mvec_e01235i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01245i.
 */
public static Mvec e01245i(){
    return new Mvec(Mvec_h.Mvec_e01245i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 01345i.
 */
public static Mvec e01345i(){
    return new Mvec(Mvec_h.Mvec_e01345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 02345i.
 */
public static Mvec e02345i(){
    return new Mvec(Mvec_h.Mvec_e02345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 12345i.
 */
public static Mvec e12345i(){
    return new Mvec(Mvec_h.Mvec_e12345i());
}
/**
 * @return a multivector that contains only the unit basis k-vector 012345i.
 */
public static Mvec e012345i(){
    return new Mvec(Mvec_h.Mvec_e012345i());
}


    /**
     * Returns a multivector that only contains the coefficient associated with the pseudoscalar.
     * @return An empty Mvec if the requested element is not part of the multivector,
     * or the multivector that contains only this element if present in the current multivector.
     */
    public static Mvec I() {
        return new Mvec(Mvec_h.Mvec_I());
    }


    // ops "value-like"
    /**
     * Defines the addition between two Mvec.
     * @param b The second operand of type Mvec.
     * @return The result of {@code this + b}.
     */
    public Mvec add(Mvec b) {
        return new Mvec(Mvec_h.Mvec_add(seg(), b.seg()));
    }

    /**
     * Defines the addition between an Mvec and a scalar.
     * @param s The second operand (scalar).
     * @return The result of {@code this + s}.
     */
    public Mvec add(double s) {
        return new Mvec(Mvec_h.Mvec_add_scalar(seg(), s));
    }

    // public Mvec radd(double s) {
    // return new Mvec(Mvec_h.Mvec_scalar_add(s, seg()));
    // }

    /**
     * Defines the geometric product between two multivectors.
     * @param b A multivector.
     * @return The result of {@code this * b}.
     */
    public Mvec mul(Mvec b) {
        return new Mvec(Mvec_h.Mvec_mul(seg(), b.seg()));
    }

    /**
     * Defines the geometric product between a multivector and a scalar.
     * @param s A scalar.
     * @return The result of {@code this * s}.
     */
    public Mvec mul(double s) {
        return new Mvec(Mvec_h.Mvec_mul_scalar(seg(), s));
    }

    /**
     * Defines the geometric product between a scalar and a multivector.
     * @param s A scalar.
     * @return The result of {@code s * this}.
     */
    public Mvec rmul(double s) {
        return new Mvec(Mvec_h.Mvec_scalar_mul(s, seg()));
    }

    /**
     * Defines the outer product between two multivectors.
     * @param b A multivector.
     * @return The result of {@code this ^ b}.
     */
    public Mvec outer(Mvec b) {
        return new Mvec(Mvec_h.Mvec_outer(seg(), b.seg()));
    }

    /**
     * Defines the inner product between two multivectors.
     * @param b A multivector.
     * @return The result of {@code this . b}.
     */
    public Mvec inner(Mvec b) {
        return new Mvec(Mvec_h.Mvec_inner(seg(), b.seg()));
    }

    /**
     * Defines the left contraction between two multivectors.
     * @param b A multivector.
     * @return The left contraction {@code this < b}.
     */
    public Mvec leftContraction(Mvec b) {
        return new Mvec(Mvec_h.Mvec_left_contraction(seg(), b.seg()));
    }

    /**
     * Defines the right contraction between two multivectors.
     * @param b A multivector.
     * @return The right contraction {@code this > b}.
     */
    public Mvec rightContraction(Mvec b) {
        return new Mvec(Mvec_h.Mvec_right_contraction(seg(), b.seg()));
    }

    /**
     * The L2-norm of the multivector, which is sqrt(abs(mv.mv)).
     * @return The L2-norm of the multivector (as a double).
     */
    public double norm() {
        return Mvec_h.Mvec_norm(seg());
    }

    public int[] grades() {
        // to get grades vector, we need to temporary allocate a C array, and
        // copy it to a java array
        int count = (int) Mvec_h.Mvec_get_grades_count(seg());
        if (count == 0) return new int[0];

        try (Arena arena = Arena.ofConfined()) {
            long elemSize = ValueLayout.JAVA_INT.byteSize();
            long bytes    = elemSize * (long) count;

            MemorySegment out = arena.allocate(bytes, ValueLayout.JAVA_INT.byteAlignment());

            int written = Mvec_h.Mvec_copy_grades(seg(), out, count);
            int n = Math.min(written, count);

            // copy to int[]
            int[] res = new int[n];
            for (int i = 0; i < n; i++) {
                res[i] = out.getAtIndex(ValueLayout.JAVA_INT, i);
            }
            return res;
        }
    }

    /**
     * Returns the (highest) grade of the multivector.
     * @return The highest grade of the multivector.
     */
    public int highestGrade() {
        return Mvec_h.Mvec_get_highest_grade(seg());
    }

    /**
     * Overloads the casting operator, allowing for conversion to a scalar.
     * For example, {@code double a = mv.toScalar();}
     * @return The scalar part of the multivector.
     */
    public double toScalar() {
        return Mvec_h.Mvec_to_scalar(seg());
    }

    /**
     * Overloads the [] operator to assign a basis blade to a multivector.
     * For example, {@code mv.set(Basis.E12, 42.0);}
     * @param i The basis vector index related to the query.
     * @param v The coefficient of the multivector corresponding to the "i" component.
     */
    public void set(int i, double v) {
        Mvec_h.Mvec_set_coeff(seg(), i, v);
    }

    /**
     * Overloads the [] operator to copy a basis blade of this multivector.
     * For example, {@code double a = mv.get(Basis.E12);}
     * @param i The basis vector index related to the query.
     * @return The coefficient of the multivector corresponding to the "i" component.
     */
    public double get(int i) {
        return Mvec_h.Mvec_get_coeff(seg(), i);
    }

    /**
     * Displays the multivector data (per grade value).
     */
    public void display() {
        Mvec_h.Mvec_display(seg());
    }

    /**
     * Checks if a multivector is empty, i.e., corresponds to 0.
     * @return True if the multivector is empty, else False.
     */
    public boolean isEmpty() {
        return Mvec_h.Mvec_is_empty(seg());
    }

    /**
     * Completely erases the content of a multivector.
     */
    public void clear() {
        Mvec_h.Mvec_clear(seg(), -1);
    }

    /**
     * Partialy erases the content of a multivector.
     * If {@code grade < 0}, erases the entire multivector; otherwise,
     * only erases the part of grade "grade".
     */
    public void clear(int grade) {
        Mvec_h.Mvec_clear(seg(), grade);
    }

    /**
     * Returns the element of the multivector at the specified index.
     * @param idx The index of the element.
     * @return The element of the Mvec at the given index.
     */
    public double at(int idx) {
        return get(idx);
    }
    
    /**
     * Modifies the element of the multivector at the specified index.
     * @param idx The index of the element.
     * @param val The value to set.
     */
    public void at(int idx, double val) {
        set(idx, val);
    }

    /**
     * Closes the native resources associated with this Mvec.
     */
    @Override
    public void close() {
        cleanable.clean();
    }

    /**
     * Defines the standard basis blades for this algebra.
     */
    public interface Basis {
        int SCALAR = 0;
    int E0 = 1;
    int E1 = 2;
    int E2 = 4;
    int E3 = 8;
    int E4 = 16;
    int E5 = 32;
    int Ei = 64;
    int E01 = 3;
    int E02 = 5;
    int E03 = 9;
    int E04 = 17;
    int E05 = 33;
    int E0i = 65;
    int E12 = 6;
    int E13 = 10;
    int E14 = 18;
    int E15 = 34;
    int E1i = 66;
    int E23 = 12;
    int E24 = 20;
    int E25 = 36;
    int E2i = 68;
    int E34 = 24;
    int E35 = 40;
    int E3i = 72;
    int E45 = 48;
    int E4i = 80;
    int E5i = 96;
    int E012 = 7;
    int E013 = 11;
    int E014 = 19;
    int E015 = 35;
    int E01i = 67;
    int E023 = 13;
    int E024 = 21;
    int E025 = 37;
    int E02i = 69;
    int E034 = 25;
    int E035 = 41;
    int E03i = 73;
    int E045 = 49;
    int E04i = 81;
    int E05i = 97;
    int E123 = 14;
    int E124 = 22;
    int E125 = 38;
    int E12i = 70;
    int E134 = 26;
    int E135 = 42;
    int E13i = 74;
    int E145 = 50;
    int E14i = 82;
    int E15i = 98;
    int E234 = 28;
    int E235 = 44;
    int E23i = 76;
    int E245 = 52;
    int E24i = 84;
    int E25i = 100;
    int E345 = 56;
    int E34i = 88;
    int E35i = 104;
    int E45i = 112;
    int E0123 = 15;
    int E0124 = 23;
    int E0125 = 39;
    int E012i = 71;
    int E0134 = 27;
    int E0135 = 43;
    int E013i = 75;
    int E0145 = 51;
    int E014i = 83;
    int E015i = 99;
    int E0234 = 29;
    int E0235 = 45;
    int E023i = 77;
    int E0245 = 53;
    int E024i = 85;
    int E025i = 101;
    int E0345 = 57;
    int E034i = 89;
    int E035i = 105;
    int E045i = 113;
    int E1234 = 30;
    int E1235 = 46;
    int E123i = 78;
    int E1245 = 54;
    int E124i = 86;
    int E125i = 102;
    int E1345 = 58;
    int E134i = 90;
    int E135i = 106;
    int E145i = 114;
    int E2345 = 60;
    int E234i = 92;
    int E235i = 108;
    int E245i = 116;
    int E345i = 120;
    int E01234 = 31;
    int E01235 = 47;
    int E0123i = 79;
    int E01245 = 55;
    int E0124i = 87;
    int E0125i = 103;
    int E01345 = 59;
    int E0134i = 91;
    int E0135i = 107;
    int E0145i = 115;
    int E02345 = 61;
    int E0234i = 93;
    int E0235i = 109;
    int E0245i = 117;
    int E0345i = 121;
    int E12345 = 62;
    int E1234i = 94;
    int E1235i = 110;
    int E1245i = 118;
    int E1345i = 122;
    int E2345i = 124;
    int E012345 = 63;
    int E01234i = 95;
    int E01235i = 111;
    int E01245i = 119;
    int E01345i = 123;
    int E02345i = 125;
    int E12345i = 126;
    int E012345i = 127;

    }
}
