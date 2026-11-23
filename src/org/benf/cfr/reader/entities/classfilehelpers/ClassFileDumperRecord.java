package org.benf.cfr.reader.entities.classfilehelpers;

import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class ClassFileDumperRecord extends AbstractClassFileDumper {

    private static final AccessFlag[] dumpableAccessFlagsClass = new AccessFlag[]{
            AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_ABSTRACT
    };

    public ClassFileDumperRecord(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, InnerClassDumpType innerClassDumpType, Dumper d) {
        d.keyword(getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsClass));

        d.keyword("record ");
        c.dumpClassIdentity(d);
        d.print("(");
        List<ClassFileField> fields = Functional.filter(c.getFields(), new Predicate<ClassFileField>() {
                    @Override
                    public boolean test(ClassFileField in) {
                        return !in.getField().testAccessFlag(AccessFlag.ACC_STATIC);
                    }
                });
        boolean first = true;
        boolean varargs = hasCanonicalVarargsConstructor(c, fields);
        for (int index = 0; index < fields.size(); ++index) {
            first = StringUtils.comma(first, d);
            fields.get(index).dumpAsRecord(d, c, varargs && index == fields.size() - 1);
        }
        d.print(") ");
        dumpImplements(d, c.getClassSignature());
    }

    private static boolean hasCanonicalVarargsConstructor(ClassFile classFile, List<ClassFileField> fields) {
        StringBuilder descriptor = new StringBuilder("(");
        for (ClassFileField field : fields) {
            descriptor.append(field.getField().getDescriptor());
        }
        descriptor.append(")V");

        for (Method constructor : classFile.getConstructors()) {
            MethodPrototype prototype = constructor.getMethodPrototype();
            if (prototype.isVarArgs() && descriptor.toString().equals(prototype.getOriginalDescriptor())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Dumper dump(ClassFile classFile, InnerClassDumpType innerClass, Dumper d) {
        if (!d.canEmitClass(classFile.getClassType())) return d;

        if (!innerClass.isInnerClass()) {
            dumpTopHeader(classFile, d, true);
            dumpImports(d, classFile);
        }

        dumpComments(classFile, d);
        dumpAnnotations(classFile, d);
        dumpHeader(classFile, innerClass, d);
        d.separator("{").newln();
        d.indent(1);
        boolean first = true;

        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            if (!field.shouldNotDisplay()) {
                field.dump(d, classFile);
                first = false;
            }
        }
        dumpMethods(classFile, d, first, true);
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        d.separator("}").newln();

        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}
