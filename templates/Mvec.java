package org.garamon.project_namespace;

import java.lang.ref.Cleaner;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import org.garamon.project_namespace.Mvec_h;

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
    

project_static_multivector_one_component

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
project_basis_vector_index
    }
}
