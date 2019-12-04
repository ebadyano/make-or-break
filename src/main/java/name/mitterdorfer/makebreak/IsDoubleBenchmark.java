package name.mitterdorfer.makebreak;

import org.openjdk.jmh.annotations.*;

import java.util.Objects;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Loosely based on http://stackoverflow.com/questions/11227809/why-is-processing-a-sorted-array-faster-than-an-unsorted-array
 *
 * This benchmark demonstrates the effects of the branch prediction unit. Based on the benchmark parameter
 * <code>randomizationProbability</code> the benchmark will produce more or less branch mispredictions due to
 * random data in the array.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IsDoubleBenchmark {

    private char [][] nums;
    @Param({"Empty", "Double", "Decimal" , "DoubleScientific", "DecimalScientific", "DoubleLead0"})
    public String numberSet;

    @Setup
    public void setUp() {
        nums = new char[100][];
        int start = 1612253458;
        //start = 1;
        String num = "";
        for (int idx = 0; idx < nums.length; idx++) {
            start++;
            while (start%10 == 0)
                start++;
            if (numberSet.equals("Empty"))
                num  = "";
            else if (numberSet.equals("Double"))
                num  = start + ".7123";
            else if (numberSet.equals("Decimal"))
                num = start + ".7123565";
            else if (numberSet.equals("DoubleScientific"))
                num = (double)(start/1000000000.0) + "71235E9";
            else if (numberSet.equals("DoubleLead0"))
                num =   (double)(start/10000000000000000.0) + "";
            else
                num = num = "0.00000" + start;

            nums[idx] = num.toCharArray();

        }
    }


    @Benchmark
    public boolean isDoubleFastOrg()
    {   boolean result = false;
    for (char [] chars : nums) {
        result =  IsDoubleBenchmark.isDouble(chars, 0, chars.length);
    }
    return result;
    }

    @Benchmark
    public boolean isDoubleSlowOrg()
    {   boolean result = false;
    for (char [] chars : nums) {
        result =  IsDoubleBenchmark.slowIsDouble(chars, 0, chars.length);
    }
    return result;
    }

    @Benchmark
    public boolean isDoubleSlowOpt()
    {   boolean result = false;
    for (char [] chars : nums) {
        result = IsDoubleBenchmark.slowIsDoubleOpt(chars, 0, chars.length);
    }
    return result;
    }

    @Benchmark
    public boolean isDoubleFastOpt()
    {   boolean result = false;
    for (char [] chars : nums) {
        result =  IsDoubleBenchmark.isDoubleOpt(chars, 0, chars.length);
    }
    return result;
    }

    static boolean isDouble(char[] chars, int charsOff, int charsLen) {

        Objects.checkFromIndexSize(charsOff, charsLen, chars.length);
        // if (charsLen <= 16) return true;
        if (charsLen <= 17) { // 15 significant digits, plus '.' and '-'
            // Avoid numbers that use a scientific notation because they might
            // be short yet have an exponent that is greater than the maximum
            // double exponent, eg. 9E999.
            boolean scientificNotation = false;

            int numSigDigits = 0;
            for (int i = charsOff, end = charsOff + charsLen; i < end; ++i) {
                char c = chars[i];
                if (c >= '0' && c <= '9') {
                    numSigDigits++;
                } else if (c != '-' && c != '.') {
                    scientificNotation = true;
                    break;
                }
            }
            if (scientificNotation == false && numSigDigits <= 15) { // Fast path
                // Doubles have 53 bits of mantissa including the implicit bit.
                // If a String with 15 significant digits or less was not the
                // string representation of a double, it would mean that two
                // consecutive doubles would differ (relatively) by more than
                // 10^-15, which is impossible since 10^-15 > 2^-53.
                return true;
            }
        }
        return slowIsDouble(chars, charsOff, charsLen);
    }

    // pkg-private for testing
    static boolean slowIsDouble(char[] chars, int charsOff, int charsLen) {
        try {
            BigDecimal bigDec = new BigDecimal(chars, charsOff, charsLen);
            double asDouble = bigDec.doubleValue();
            if (Double.isFinite(asDouble) == false) {
                return false;
            }
            // Don't use equals since it returns false for decimals that have the
            // same value but different scales.
            return bigDec.compareTo(new BigDecimal(Double.toString(asDouble))) == 0;
        } catch (NumberFormatException e) {
            // We need to return true for NaN and +/-Infinity
            // For malformed strings, the return value is undefined, so true is fine too.
            return true;
        }
    }

    // pkg-private for testing
    static boolean slowIsDoubleOpt(char[] chars, int charsOff, int charsLen) {
        try {
            BigDecimal bigDec = new BigDecimal(chars, charsOff, charsLen);
            if (bigDec.precision() <= 15)
                return true;
            double asDouble = bigDec.doubleValue();
            if (Double.isFinite(asDouble) == false) {
                return false;

            }
            // Don't use equals since it returns false for decimals that have the
            // same value but different scales.
            return bigDec.compareTo(new BigDecimal(Double.toString(asDouble))) == 0;
        } catch (NumberFormatException e) {
            // We need to return true for NaN and +/-Infinity
            // For malformed strings, the return value is undefined, so true is fine too.
            return true;
        }
    }

    static boolean isDoubleOpt(char[] chars, int charsOff, int charsLen) {
        Objects.checkFromIndexSize(charsOff, charsLen, chars.length);
        if (charsLen <= 17) { // 15 significant digits, plus '.' and '-'
            // Avoid numbers that use a scientific notation because they might
            // be short yet have an exponent that is greater than the maximum
            // double exponent, eg. 9E999.
            boolean scientificNotation = false;
            int numSigDigits = 0;
            for (int i = charsOff, end = charsOff + charsLen; i < end; ++i) {
                char c = chars[i];
                if (c >= '0' && c <= '9') {
                    numSigDigits++;
                } else if (c != '-' && c != '.') {
                    scientificNotation = true;
                    break;
                }
            }
            if (scientificNotation == false && numSigDigits <= 15) { // Fast path
                // Doubles have 53 bits of mantissa including the implicit bit.
                // If a String with 15 significant digits or less was not the
                // string representation of a double, it would mean that two
                // consecutive doubles would differ (relatively) by more than
                // 10^-15, which is impossible since 10^-15 > 2^-53.
                return true;
            }
        }
        return slowIsDoubleOpt(chars, charsOff, charsLen);
    }

}

