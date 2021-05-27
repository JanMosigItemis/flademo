package de.itemis.mosig.flademo;

import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.toCString;

import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.LibraryLookup.Symbol;
import jdk.incubator.foreign.MemoryAddress;

public class FlaDemo {
    private static final FunctionDescriptor STRLEN_C_FUNC_DESCR =
        System.getProperty("os.name").toLowerCase().contains("win") ? FunctionDescriptor.of(C_LONG_LONG, C_POINTER)
            : FunctionDescriptor.of(C_LONG, C_POINTER);
    private static final MethodType STRLEN_JAVA_METHOD_DESCR = MethodType.methodType(long.class, MemoryAddress.class);

    public static void main(String[] args) {
        Objects.requireNonNull(args, "args");

        IllegalArgumentException noFirstArgment = new IllegalArgumentException("Please provide a test string as first argument.");

        if (args.length == 0) {
            throw noFirstArgment;
        }

        if (args[0] == null) {
            throw noFirstArgment;
        }

        String testStr = args[0];
        long expectedStrLength = testStr.length();

        long actualStrLength = strlen(testStr);
        if (actualStrLength != expectedStrLength) {
            throw new RuntimeException("Encountered unexpected string length. Was: " + actualStrLength + " should be: " + expectedStrLength);
        } else {
            System.out.println("String '" + testStr + "' has " + actualStrLength + " characters.");
        }
    }

    public static long strlen(String str) {
        var linker = CLinker.getInstance();
        var cLib = LibraryLookup.ofDefault();
        var strlenCFuncRef = loadSymbol(cLib, "strlen");
        var strlenJavaMethodRef = linker.downcallHandle(strlenCFuncRef, STRLEN_JAVA_METHOD_DESCR, STRLEN_C_FUNC_DESCR);
        var cStr = toCString(str, StandardCharsets.UTF_8);

        long result = -1;
        try {
            result = (long) strlenJavaMethodRef.invokeExact(cStr.address());
        } catch (WrongMethodTypeException e) {
            throw e;
        } catch (Throwable t) {
            var errMsg = t.getMessage() == null ? "No further information" : t.getMessage();
            throw new RuntimeException("Calling the library function caused a problem: " + t.getClass().getSimpleName() + ": " + errMsg, t);
        }

        return result;
    }

    private static Symbol loadSymbol(LibraryLookup lib, String symbolName) {
        return lib.lookup(symbolName)
            .orElseThrow(() -> new RuntimeException("Could not find symbol '" + symbolName + "' in library '" + lib.toString() + "'."));
    }
}
