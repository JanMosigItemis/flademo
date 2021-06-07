package de.itemis.mosig.flademo;

import static java.nio.ByteOrder.nativeOrder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.toCString;
import static jdk.incubator.foreign.MemoryAddress.NULL;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.LibraryLookup.Symbol;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

public class FlaDemo {
    private static final FunctionDescriptor STRLEN_C_FUNC_DESCR =
        System.getProperty("os.name").toLowerCase().contains("win") ? FunctionDescriptor.of(C_LONG_LONG, C_POINTER)
            : FunctionDescriptor.of(C_LONG, C_POINTER);
    private static final MethodType STRLEN_JAVA_METHOD_DESCR = MethodType.methodType(long.class, MemoryAddress.class);

    private static final long PCSC_SCOPE_SYSTEM = 2;

    // Stored here for reference
    @SuppressWarnings("unused")
    private static final long PCSC_SCOPE_USER = 0;

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

    public static void sCardListReaders() {
        var linker = CLinker.getInstance();
        var cLib = LibraryLookup.ofLibrary("winscard");

        var establishCtxFunc = loadSymbol(cLib, "SCardEstablishContext");
        var establishCtxFuncDescr = FunctionDescriptor.of(C_LONG_LONG, C_LONG_LONG, C_POINTER, C_POINTER, C_POINTER);
        var establishCtxMethodType = MethodType.methodType(long.class, long.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class);
        var establishCtxMethod = linker.downcallHandle(establishCtxFunc, establishCtxMethodType, establishCtxFuncDescr);

        var listReadersFunc = loadSymbol(cLib, "SCardListReadersA");
        var listReadersFuncDescr = FunctionDescriptor.of(C_LONG_LONG, C_POINTER, C_POINTER, C_POINTER, C_POINTER);
        var listReadersMethodType = MethodType.methodType(long.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class);
        var listReadersMethod = linker.downcallHandle(listReadersFunc, listReadersMethodType, listReadersFuncDescr);

        var freeMemFunc = loadSymbol(cLib, "SCardFreeMemory");
        var freeMemFuncDescr = FunctionDescriptor.of(C_LONG_LONG, C_POINTER, C_POINTER);
        var freeMemMethodType = MethodType.methodType(long.class, MemoryAddress.class, MemoryAddress.class);
        var freeMemMethod = linker.downcallHandle(freeMemFunc, freeMemMethodType, freeMemFuncDescr);

        var releaseCtxFunc = loadSymbol(cLib, "SCardReleaseContext");
        var releaseCtxFuncDescr = FunctionDescriptor.of(C_LONG_LONG, C_POINTER);
        var releaseCtxMethodType = MethodType.methodType(long.class, MemoryAddress.class);
        var releaseCtxMethod = linker.downcallHandle(releaseCtxFunc, releaseCtxMethodType, releaseCtxFuncDescr);

        MemorySegment cardCtxPtrSeg = null;
        MemorySegment readerListSeg = null;
        MemorySegment readerListLengthSeg = null;
        MemoryAddress addrOfCardCtxPtr = null;
        MemoryAddress cardCtxPtr = null;
        MemoryAddress readerListPtr = null;

        try {
            cardCtxPtrSeg = MemorySegment.allocateNative(MemoryLayouts.ADDRESS);
            addrOfCardCtxPtr = cardCtxPtrSeg.address();
            System.out.println("establishCtx: " + (long) establishCtxMethod.invokeExact(PCSC_SCOPE_SYSTEM, NULL, NULL, addrOfCardCtxPtr));
            long addr = cardCtxPtrSeg.asByteBuffer().order(nativeOrder()).getLong();
            cardCtxPtr = MemoryAddress.ofLong(addr);
            System.out.println("cardCtxPtr: " + Long.toHexString(addr));

            readerListSeg = MemorySegment.allocateNative(MemoryLayouts.ADDRESS);
            readerListLengthSeg = MemorySegment.allocateNative(MemoryLayouts.BITS_32_LE);
            readerListLengthSeg.asByteBuffer().putInt(-1);
            System.out
                .println("listReaders: " + (long) listReadersMethod.invokeExact(cardCtxPtr, NULL, readerListSeg.address(), readerListLengthSeg.address()));

            addr = readerListSeg.asByteBuffer().order(nativeOrder()).getLong();
            readerListPtr = MemoryAddress.ofLong(addr);
            System.out.println("readerListPtr: " + Long.toHexString(addr));
            System.out.println("readerListLength: " + readerListLengthSeg.asByteBuffer().order(nativeOrder()).getInt());

            addr = readerListSeg.asByteBuffer().order(nativeOrder()).getLong();
            String firstReader = CLinker.toJavaStringRestricted(MemoryAddress.ofLong(addr), UTF_8);
            String secondReader =
                CLinker.toJavaStringRestricted(MemoryAddress.ofLong(addr + 1 + firstReader.getBytes(UTF_8).length), UTF_8);
            System.out.println("Reader list contents: " + firstReader);
            System.out.println("Reader list contents: " + secondReader);
        } catch (WrongMethodTypeException e) {
            throw e;
        } catch (Throwable t) {
            var errMsg = t.getMessage() == null ? "No further information" : t.getMessage();
            throw new RuntimeException("Calling the library function caused a problem: " + t.getClass().getSimpleName() + ": " + errMsg, t);
        } finally {
            if (readerListLengthSeg != null) {
                try {
                    System.out.println("freeMem: " + (long) freeMemMethod.invokeExact(cardCtxPtr, readerListPtr));
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    readerListLengthSeg.close();
                    readerListSeg.close();
                }
            }

            if (cardCtxPtrSeg != null) {
                try {
                    System.out.println("releaseCtx: " + (long) releaseCtxMethod.invokeExact(cardCtxPtr));
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    cardCtxPtrSeg.close();
                }
            }
        }
    }

    public static int assuan_new() {
        var linker = CLinker.getInstance();
        var cLib = LibraryLookup.ofPath(Path.of(URI.create("file:///C:/devtools/gnupg/bin_64/libassuan6-0.dll")));

        var cFuncRef = loadSymbol(cLib, "assuan_new");
        FunctionDescriptor cFuncDescr = FunctionDescriptor.of(C_INT, C_POINTER);
        MethodType javaMethodDescr = MethodType.methodType(int.class, MemoryAddress.class);
        var javaMethodRef = linker.downcallHandle(cFuncRef, javaMethodDescr, cFuncDescr);

        MemoryAddress addrOfCtxPtr = null;
        MemoryAddress ctxPtr = null;
        MemorySegment ctxPtrSeg = null;

        MemorySegment data_cbC = null;
        MemorySegment inquire_cbC = null;
        MemorySegment status_cbC = null;

        int errorCode = -1;
        try {
            ctxPtrSeg = MemorySegment.allocateNative(MemoryLayouts.ADDRESS);
            addrOfCtxPtr = ctxPtrSeg.address();

            errorCode = (int) javaMethodRef.invokeExact(addrOfCtxPtr);
            System.out.println("assuan_new: " + (errorCode & 0xFFFF));

            long addr = ctxPtrSeg.asByteBuffer().order(nativeOrder()).getLong();
            ctxPtr = MemoryAddress.ofLong(addr);

            var socketFuncRef = loadSymbol(cLib, "assuan_socket_connect");
            var socketFuncDescr = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT,
                C_INT);
            var socketMethodDescr = MethodType.methodType(int.class, MemoryAddress.class,
                MemoryAddress.class, int.class, int.class);
            var socketMethodRef = linker.downcallHandle(socketFuncRef, socketMethodDescr,
                socketFuncDescr);
            errorCode = (int) socketMethodRef.invokeExact(ctxPtr,
                CLinker.toCString("C:\\Users\\mosig_user\\.gnupg\\S.scdaemon", UTF_8).address(), -1, 0);
            // Lowest 16 bits are error code, see gpg-error.h
            System.out.println("assuan_socket_connect: " + (errorCode & 0xFFFF));

            var data_cbJava = MethodHandles.lookup().findStatic(FlaDemo.class, "data_cb",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, long.class));
            data_cbC = linker.upcallStub(data_cbJava, FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_LONG_LONG));

            var inquire_cbJava = MethodHandles.lookup().findStatic(FlaDemo.class, "inquire_cb",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class));
            inquire_cbC = linker.upcallStub(inquire_cbJava, FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));

            var status_cbJava = MethodHandles.lookup().findStatic(FlaDemo.class, "status_cb",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class));
            status_cbC = linker.upcallStub(status_cbJava, FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));

            var transactFuncRef = loadSymbol(cLib, "assuan_transact");
            var transactFuncDescr = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER);
            var transactMethodDescr = MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class,
                MemoryAddress.class, MemoryAddress.class, MemoryAddress.class, MemoryAddress.class);
            var transactMethodRef = linker.downcallHandle(transactFuncRef, transactMethodDescr,
                transactFuncDescr);
            errorCode = (int) transactMethodRef.invokeExact(ctxPtr,
                CLinker.toCString("GETATTR LOGIN-DATA", UTF_8).address(), data_cbC.address(), NULL, inquire_cbC.address(), NULL,
                status_cbC.address(), NULL);
            // Lowest 16 bits are error code, see gpg-error.h
            System.out.println("assuan_transact: " + (errorCode & 0xFFFF));

            errorCode = (int) transactMethodRef.invokeExact(ctxPtr,
                CLinker.toCString("SERIALNO", UTF_8).address(), data_cbC.address(), NULL, inquire_cbC.address(), NULL,
                status_cbC.address(), NULL);
            // Lowest 16 bits are error code, see gpg-error.h
            System.out.println("assuan_transact: " + (errorCode & 0xFFFF));
        } catch (WrongMethodTypeException e) {
            throw e;
        } catch (Throwable t) {
            var errMsg = t.getMessage() == null ? "No further information" : t.getMessage();
            throw new RuntimeException("Calling the library function caused a problem: " + t.getClass().getSimpleName() + ": " + errMsg, t);
        } finally {
            if (ctxPtr != null) {
                var cReleaseFuncRef = loadSymbol(cLib, "assuan_release");
                FunctionDescriptor cReleaseFuncDescr = FunctionDescriptor.ofVoid(C_POINTER);
                MethodType javaReleaseMethodDescr = MethodType.methodType(void.class,
                    MemoryAddress.class);
                var javaReleaseMethodRef = linker.downcallHandle(cReleaseFuncRef,
                    javaReleaseMethodDescr, cReleaseFuncDescr);
                try {
                    javaReleaseMethodRef.invokeExact(ctxPtr);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    ctxPtrSeg.close();
                }
            }

            if (data_cbC != null) {
                data_cbC.close();
            }
            if (inquire_cbC != null) {
                inquire_cbC.close();
            }
            if (status_cbC != null) {
                status_cbC.close();
            }
        }

        return errorCode;
    }

    public static int data_cb(MemoryAddress allLines, MemoryAddress currentLine, long lineLength) {
        System.out.println("data_cb");
        System.out.println(CLinker.toJavaStringRestricted(currentLine, UTF_8));
        return 0;
    }

    public static int inquire_cb(MemoryAddress allLines, MemoryAddress currentLine) {
        System.out.println("inquire_cb");
        System.out.println(CLinker.toJavaStringRestricted(currentLine, UTF_8));
        return 0;
    }

    public static int status_cb(MemoryAddress allLines, MemoryAddress currentLine) {
        System.out.println("status_cb");
        System.out.println(CLinker.toJavaStringRestricted(currentLine, UTF_8));
        return 0;
    }

    public static long strlen(String str) {
        var linker = CLinker.getInstance();
        var cLib = LibraryLookup.ofDefault();
        var strlenCFuncRef = loadSymbol(cLib, "strlen");
        var strlenJavaMethodRef = linker.downcallHandle(strlenCFuncRef, STRLEN_JAVA_METHOD_DESCR, STRLEN_C_FUNC_DESCR);
        var cStr = toCString(str, UTF_8);

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
