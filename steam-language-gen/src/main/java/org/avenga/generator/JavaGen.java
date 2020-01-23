package org.avenga.generator;

import org.avenga.exception.ClassGeneratorException;
import org.avenga.exception.StreamProcessingException;
import org.avenga.parser.node.ClassNode;
import org.avenga.parser.node.EnumNode;
import org.avenga.parser.node.Node;
import org.avenga.parser.node.PropertyNode;
import org.avenga.parser.symbol.StrongSymbol;
import org.avenga.parser.symbol.Symbol;
import org.avenga.parser.symbol.WeakSymbol;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaGen implements Closeable, Flushable {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?[0-9].*?L?");
    private static final String DEFAULT_TYPE = "uint";

    private static final Map<String, TypeInfo> WEAK_TYPES = new LinkedHashMap<>(){{
        put("byte", new TypeInfo(1, "byte"));
        put("short", new TypeInfo(2, "short"));
        put("ushort", new TypeInfo(2, "short"));
        put("int", new TypeInfo(4, "int"));
        put("uint", new TypeInfo(4, "int"));
        put("long", new TypeInfo(8, "long"));
        put("ulong", new TypeInfo(8, "long"));
    }};

    private JavaFileWriter writer;
    private final Node node;
    private String _package;
    private String basePackage;
    private File destination;
    private Set<String> flagEnums;

    public JavaGen(Node node, String _package, String basePackage, File destination, Set<String> flagEnums) {
        this.node = node;
        this._package = _package;
        this.basePackage = basePackage;
        this.destination = destination;
        this.flagEnums = flagEnums;
    }

    public void emit() {
        if (node instanceof ClassNode && !((ClassNode) node).isEmit()) {
            return;
        }

        if (!destination.exists() && !destination.isDirectory() && !destination.mkdirs()) {
            throw new IllegalStateException("Couldn't create folders");
        }

        var file = new File(destination, node.getName() + ".java");

        try {
            this.writer = new JavaFileWriter(file);
            writePackage(_package);
            writer.writeln();
            writeImports();
            writer.writeln();
            writeClass(node);
        } catch (Exception e) {
            throw new ClassGeneratorException(e.getMessage(), e);
        }
    }

    private void writeImports() throws IOException {
        if (node instanceof ClassNode) {
            var classNode = (ClassNode) node;
            var imports = new HashSet<String>();

            imports.add("java.io.IOException");
            imports.add("java.io.InputStream");
            imports.add("java.io.OutputStream");
            imports.add(getPackageName("util.stream.BinaryReader"));
            imports.add(getPackageName("util.stream.BinaryWriter"));
            if (classNode.getIdent() != null) {
                if (node.getName().contains("MsgGC")) {
                    imports.add(getPackageName("base.GCSerializableMessage"));
                } else {
                    imports.add(getPackageName("base.SteamSerializableMessage"));
                    imports.add(getPackageName("enums.EMsg"));
                }
            } else if (node.getName().contains("Hdr")) {
                if (node.getName().contains("MsgGC")) {
                    imports.add(getPackageName("base.GCSerializableHeader"));
                } else {
                    imports.add(getPackageName("base.SteamSerializableHeader"));
                    imports.add(getPackageName("enums.EMsg"));
                }
            } else {
                imports.add(getPackageName("base.SteamSerializable"));
            }

            for (Node child : (classNode.getChildNodes())) {
                var prop = (PropertyNode) child;
                var typeStr = getType(prop.getType());

                if (flagEnums.contains(typeStr)) {
                    imports.add("java.util.EnumSet");
                }

                if ("steamidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                    imports.add(getPackageName("types.SteamID"));
                } else if ("gameidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                    imports.add(getPackageName("types.GameID"));
                } else if ("proto".equals(prop.getFlags())) {
                    imports.add(getPackageName("protobufs.steamclient.SteammessagesBase.CMsgProtoBufHeader"));
                } else if ("protomask".equals(prop.getFlags())) {
                    imports.add(getPackageName("enums.EMsg"));
                    imports.add(getPackageName("util.MessageUtil"));
                }

                if (prop.getType() instanceof StrongSymbol) {
                    var strongSymbol = (StrongSymbol) prop.getType();
                    if (strongSymbol.getClazz() instanceof EnumNode) {
                        imports.add(getPackageName("enums." + strongSymbol.getClazz().getName()));
                    }
                }
            }

            var sortedImports = new ArrayList<>(imports);
            Collections.sort(sortedImports);
            String currentGroup = null;
            for (String imp : sortedImports) {
                var group = imp.substring(0, imp.indexOf("."));

                if (!group.equals(currentGroup)) {
                    if (currentGroup != null) {
                        writer.writeln();
                    }
                    currentGroup = group;
                }

                writer.writeln("import " + imp + ";");
            }
        } else if (node instanceof EnumNode) {
            if ("flags".equals(((EnumNode) node).getFlag())) {
                writer.writeln("import java.util.EnumSet;");
            }
        }
    }

    private void writePackage(String _package) throws IOException {
        writer.writeln("package " + _package + ";");
    }

    private void writeClass(Node node) throws IOException {
        if (node instanceof ClassNode && ((ClassNode) node).isEmit()) {
            writeMessageClass((ClassNode) node);
        } else if (node instanceof EnumNode) {
            writeEnumClass((EnumNode) node);
        }
    }

    private void writeMessageClass(ClassNode node) throws IOException {
        writeClassDef(node);
        writer.indent();
        writeClassConstructor(node);
        writeClassProperties(node);
        writeClassIdentity(node);
        writeSetterGetter(node);
        writeSerializationMethods(node);
        writer.unindent();
        writer.writeln("}");
    }

    private void writeClassDef(ClassNode node) throws IOException {
        String parent;

        if (node.getIdent() != null) {
            if (node.getName().contains("MsgGC")) {
                parent = "GCSerializableMessage";
            } else {
                parent = "SteamSerializableMessage";
            }
        } else if (node.getName().contains("Hdr")) {
            if (node.getName().contains("MsgGC")) {
                parent = "GCSerializableHeader";
            } else {
                parent = "SteamSerializableHeader";
            }
        } else {
            parent = "SteamSerializable";
        }

        if (parent != null) {
            writer.writeln("public class " + node.getName() + " implements " + parent + " {");
        } else {
            writer.writeln("public class " + node.getName() + " {");
        }

        writer.writeln();
    }

    private void writeClassIdentity(ClassNode node) throws IOException {
        if (node.getIdent() != null) {
            var sIdent = (StrongSymbol) node.getIdent();
            var suppressObsolete = false;

            if (sIdent != null) {
                var propNode = (PropertyNode) sIdent.getProperty();

                if (propNode != null && propNode.getObsolete() != null) {
                    suppressObsolete = true;
                }
            }

            if (suppressObsolete) {
                // TODO: 2018-02-19
            }

            if (node.getName().contains("MsgGC")) {
                writer.writeln("@Override");
                writer.writeln("public int getEMsg() {");
                writer.writeln("    return " + getType(node.getIdent()) + ";");
                writer.writeln("}");
            } else {
                writer.writeln("@Override");
                writer.writeln("public EMsg getEMsg() {");
                writer.writeln("    return " + getType(node.getIdent()) + ";");
                writer.writeln("}");
            }

            writer.writeln();
        } else if (node.getName().contains("Hdr")) {
            if (node.getName().contains("MsgGC")) {
                if (node.getChildNodes().stream().anyMatch(childNode -> "msg".equals(childNode.getName()))) {
                    writer.writeln("@Override");
                    writer.writeln("public void setEMsg(int msg) {");
                    writer.writeln("    this.msg = msg;");
                    writer.writeln("}");
                } else {
                    // this is required for a gc header which doesn"t have an emsg
                    writer.writeln("@Override");
                    writer.writeln("public void setEMsg(int msg) {}");
                }
            } else {
                writer.writeln("@Override");
                writer.writeln("public void setEMsg(EMsg msg) {");
                writer.writeln("    this.msg = msg;");
                writer.writeln("}");
            }
            writer.writeln();
        }
    }

    private void writeClassProperties(ClassNode node) throws IOException {

        if (node.getParent() != null) {
            var parentType = getType(node.getParent());
            writer.writeln("private " + parentType + " header;");
            writer.writeln();
        }

        for (Node child : (node.getChildNodes())) {
            var prop = (PropertyNode) child;
            var typeStr = getType(prop.getType());
            var propName = prop.getName();

            var defSym = prop.getDefaultSymbols().isEmpty() ? null : prop.getDefaultSymbols().get(0);
            var ctor = getType(defSym);

            if ("proto".equals(prop.getFlags())) {
                ctor = "CMsgProtoBufHeader.newBuilder()";
                typeStr += ".Builder";
            } else if (defSym == null) {
                if (prop.getFlagsOpt() != null && !prop.getFlagsOpt().isEmpty()) {
                    ctor = "new " + typeStr + "[" + getTypeSize(prop) + "]";
                } else {
                    ctor = "0";
                }
            }

            if (flagEnums.contains(typeStr)) {
                typeStr = "EnumSet<" + typeStr + ">";
            }

            if (NUMBER_PATTERN.matcher(ctor).matches()) {
                if ("long" == typeStr) {
                    ctor += "L";
                } else if ("byte" == typeStr) {
                    ctor = "(byte) " + ctor;
                } else if ("short" == typeStr) {
                    ctor = "(short) " + ctor;
                }

                if (prop.getType() instanceof StrongSymbol) {
                    var strongSymbol = (StrongSymbol) prop.getType();
                    if (strongSymbol.getClazz() instanceof EnumNode) {
                        ctor = strongSymbol.getClazz().getName() + ".from(" + ctor + ")";
                    }
                }
            }

            if ("const".equals(prop.getFlags())) {
                writer.writeln("public static final " + typeStr + " " + propName + " = " +getType(prop.getDefaultSymbols().get(0)) + ";");
                writer.writeln();
                continue;
            }

            if ("steamidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("private long " + propName + " = " + ctor + ";");
            } else if ("boolmarshal".equals(prop.getFlags()) && "byte".equals(typeStr)) {
                writer.writeln("private boolean " + propName + " = false;");
            } else if ("gameidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("private long " + propName + " = " + ctor + ";");
            } else {
                if (!(prop.getFlagsOpt() == null || prop.getFlagsOpt().isEmpty()) &&
                        NUMBER_PATTERN.matcher(prop.getFlagsOpt()).matches()) {
                    typeStr += "[]";
                }

                writer.writeln ("private " + typeStr + " " + propName + " = " + ctor+ ";");
            }
            writer.writeln();
        }
    }

    private void writeSetterGetter(ClassNode node) throws IOException {

        if (node.getParent() != null) {
            var parentType = getType(node.getParent());
            writer.writeln("public " + parentType + " getHeader() {");
            writer.writeln("    return this.header;");
            writer.writeln("}");
            writer.writeln();
            writer.writeln("public void getHeader(" + parentType + " header) {");
            writer.writeln("    this.header = header;");
            writer.writeln("}");
        }

        for (Node child : (node.getChildNodes())) {
            var propNode = (PropertyNode) child;
            var typeStr = getType(propNode.getType());
            var propName = propNode.getName();

            if (flagEnums.contains(typeStr)) {
                typeStr = "EnumSet<" + typeStr + ">";
            }

            if ("const".equals(propNode.getFlags())) {
                continue;
            }

            if (("proto").equals(propNode.getFlags())) {
                typeStr += ".Builder";
            }

            if ("steamidmarshal".equals(propNode.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("public SteamID get" + capitalize(propName) + "() {");
                writer.writeln("    return new SteamID(this." + propName + ");");
                writer.writeln("}");
                writer.writeln();
                writer.writeln("public void set" + capitalize(propName) + "(SteamID steamId) {");
                writer.writeln("    this." + propName + " = steamId.convertToUInt64();");
                writer.writeln("}");
            } else if ("boolmarshal".equals(propNode.getFlags()) && "byte".equals(typeStr)) {
                writer.writeln("public boolean get" + capitalize(propName) + "() {");
                writer.writeln("    return this." + propName + ";");
                writer.writeln("}");
                writer.writeln();
                writer.writeln("public void set" + capitalize(propName) + "(boolean " + propName + ") {");
                writer.writeln("    this." + propName + " = " + propName + ";");
                writer.writeln("}");
            } else if ("gameidmarshal".equals(propNode.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("public GameID get" + capitalize(propName) + "() {");
                writer.writeln("    return new GameID(this." + propName + ");");
                writer.writeln("}");
                writer.writeln();
                writer.writeln("public void set" + capitalize(propName) + "(GameID gameId) {");
                writer.writeln("    this." + propName + " = gameId.convertToUInt64();");
                writer.writeln("}");
            } else {
                if (!(propNode.getFlagsOpt() == null || propNode.getFlagsOpt().isEmpty()) &&
                        NUMBER_PATTERN.matcher(propNode.getFlagsOpt()).matches()) {
                    typeStr += "[]";
                }

                writer.writeln("public " + typeStr + " get" + capitalize(propName) + "() {");
                writer.writeln("    return this." + propName + ";");
                writer.writeln("}");
                writer.writeln();
                writer.writeln("public void set" + capitalize(propName) + "(" + typeStr + " " + propName + ") {");
                writer.writeln("    this." + propName + " = " + propName + ";");
                writer.writeln("}");
            }
            writer.writeln();
        }
    }

    private void writeClassConstructor(ClassNode node) throws IOException {
        if (node.getParent() != null) {
            writer.writeln("public " + node.getName() + "() {");
            writer.writeln("    this.header = new " + getType(node.getParent()) + "();");
            writer.writeln("    header.setMsg(getEMsg());");
            writer.writeln("}");
        }
    }

    private void writeSerializationMethods(ClassNode node) throws IOException {
        Set<String> skip = new HashSet<>();

        for (Node child : (node.getChildNodes())) {
            var prop = (PropertyNode) child;
            if ("proto".equals(prop.getFlags())) {
                skip.add(prop.getFlagsOpt());
            }
        }

        writer.writeln("@Override");
        writer.writeln("public void serialize(OutputStream stream) throws IOException {");
        writer.indent();

        writer.writeln("BinaryWriter bw = new BinaryWriter(stream);");
        writer.writeln();

        for (Node child : (node.getChildNodes())) {
            var prop = (PropertyNode) child;
            var typeStr = getType(prop.getType());
            var propName = prop.getName();

            if (skip.contains(propName)) {
                continue;
            }

            if ("protomask".equals(prop.getFlags())) {
                writer.writeln("bw.writeInt(MessageUtil.makeMessage(" + propName + ".code(), true));");
                continue;
            }

            if ("proto".equals(prop.getFlags())) {
                writer.writeln("byte[] " + propName + "Buffer = " + propName + ".build().toByteArray();");
                if (prop.getFlagsOpt() != null) {
                    writer.writeln(prop.getFlagsOpt() + " = " + propName + "Buffer.length;");
                    writer.writeln("bw.writeInt(" + prop.getFlagsOpt() + ");");
                } else {
                    writer.writeln("bw.writeInt(" + propName + "Buffer.length);");
                }
                writer.writeln("bw.write(" + propName + "Buffer);");
                continue;
            }

            if ("const".equals(prop.getFlags())) {
                continue;
            }

            if (prop.getType() instanceof StrongSymbol) {
                var strongSymbol = (StrongSymbol) prop.getType();
                if (strongSymbol.getClazz() instanceof EnumNode) {
                    var enumType = getType(((EnumNode) strongSymbol.getClazz()).getType());

                    if (flagEnums.contains(typeStr)) {
                        switch (enumType) {
                            case "long":
                                writer.writeln("bw.writeLong(" + typeStr + ".code(" + propName+ "));");
                                break;
                            case "byte":
                                writer.writeln("bw.writeByte(" + typeStr + ".code(" + propName + "));");
                                break;
                            case "short":
                                writer.writeln("bw.writeShort(" + typeStr + ".code(" + propName + "));");
                                break;
                            default:
                                writer.writeln("bw.writeInt(" + typeStr + ".code(" + propName + "));");
                                break;
                        }
                    } else {
                        switch (enumType) {
                            case "long":
                                writer.writeln("bw.writeLong(" + propName+ ".code());");
                                break;
                            case "byte":
                                writer.writeln("bw.writeByte(" + propName+ ".code());");
                                break;
                            case "short":
                                writer.writeln("bw.writeShort(" + propName+ ".code());");
                                break;
                            default:
                                writer.writeln("bw.writeInt(" + propName+ ".code());");
                                break;
                        }
                    }

                    continue;
                }
            }

            if ("steamidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("bw.writeLong(" + propName + ");");
            } else if ("boolmarshal".equals(prop.getFlags()) && "byte".equals(typeStr)) {
                writer.writeln("bw.writeBoolean(" + propName + ");");
            } else if ("gameidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln("bw.writeLong(" + propName + ");");
            } else {
                var isArray = false;
                if (!(prop.getFlagsOpt() == null || prop.getFlagsOpt().isEmpty()) &&
                        NUMBER_PATTERN.matcher(prop.getFlagsOpt()).matches()) {
                    isArray = true;
                }

                if (isArray) {
                    writer.writeln("bw.writeInt(" + propName+ ".length);");
                    writer.writeln("bw.write(" + propName + ");");
                } else {
                    switch (typeStr) {
                        case "long":
                            writer.writeln("bw.writeLong(" + propName + ");");
                            break;
                        case "byte":
                            writer.writeln("bw.writeByte(" + propName + ");");
                            break;
                        case "short":
                            writer.writeln("bw.writeShort(" + propName + ");");
                            break;
                        default:
                            writer.writeln("bw.writeInt(" + propName + ");");
                            break;
                    }
                }
            }
        }

        writer.unindent();
        writer.writeln("}");
        writer.writeln();
        writer.writeln("@Override");
        writer.writeln("public void deserialize(InputStream stream) throws IOException {");
        writer.indent();

        writer.writeln("BinaryReader br = new BinaryReader(stream);");
        writer.writeln();

        for (Node child : (node.getChildNodes())) {
            var prop = (PropertyNode) child;
            var typeStr = getType(prop.getType());
            var propName = prop.getName();

            if (skip.contains(propName)) {
                continue;
            }

            if (prop.getFlags() != null) {
                if ("protomask".equals(prop.getFlags())) {
                    writer.writeln(propName + " = MessageUtil.getMessage(br.readInt());");
                    continue;
                }

                if ("proto".equals(prop.getFlags())) {
                    if (prop.getFlagsOpt() != null) {
                        writer.writeln(prop.getFlagsOpt() + " = br.readInt();");
                        writer.writeln("byte[] " + propName+ "Buffer = br.readBytes(" + prop.getFlagsOpt() + ");");
                    } else {
                        writer.writeln("byte[] " + propName+ "Buffer = br.readBytes(br.readInt());");
                    }
                    writer.writeln(propName + " = " + typeStr + ".newBuilder().mergeFrom(" + propName+ "Buffer);");
                    continue;
                }

                if ("const".equals(prop.getFlags())) {
                    continue;
                }
            }

            if (prop.getType() instanceof StrongSymbol) {
                var strongSymbol = (StrongSymbol) prop.getType();
                if (strongSymbol.getClazz() instanceof EnumNode) {
                    String enumType = getType(((EnumNode) strongSymbol.getClazz()).getType());
                    String className = strongSymbol.getClazz().getName();

                    switch (enumType) {
                        case "long":
                            writer.writeln(propName + " = " + className + ".from(br.readLong());");
                            break;
                        case "byte":
                            writer.writeln(propName + " = " + className + ".from(br.readByte());");
                            break;
                        case "short":
                            writer.writeln(propName + " = " + className + ".from(br.readShort());");
                            break;
                        default:
                            writer.writeln(propName + " = " + className + ".from(br.readInt());");
                            break;
                    }
                    continue;
                }
            }

            if ("steamidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln( propName + " = br.readLong();");
            } else if ("boolmarshal".equals(prop.getFlags()) && "byte".equals(typeStr)) {
                writer.writeln(propName + " = br.readBoolean();");
            } else if ("gameidmarshal".equals(prop.getFlags()) && "long".equals(typeStr)) {
                writer.writeln(propName + " = br.readLong();");
            } else {
                var isArray = false;
                if (!(prop.getFlagsOpt() == null || prop.getFlagsOpt().isEmpty()) &&
                        NUMBER_PATTERN.matcher(prop.getFlagsOpt()).matches()) {
                    isArray = true;
                }

                if (isArray) {
                    writer.writeln(propName + " = br.readBytes(br.readInt());");
                } else {
                    switch (typeStr) {
                        case "long":
                            writer.writeln(propName + " = br.readLong();");
                            break;
                        case "byte":
                            writer.writeln(propName + " = br.readByte();");
                            break;
                        case "short":
                            writer.writeln(propName + " = br.readShort();");
                            break;
                        default:
                            writer.writeln(propName + " = br.readInt();");
                            break;
                    }
                }
            }
        }

        writer.unindent();
        writer.writeln("}");
    }

    private void writeEnumClass(EnumNode node) throws IOException {
        boolean flags = "flags".equals(node.getFlag());

        if (flags) {
            flagEnums.add(node.getName());
        }

        writer.writeln("public enum " + node.getName() + " {");
        writer.writeln();
        writer.indent();

        String type = node.getType() == null ? "int" : getType(node.getType());
        writeEnumProperties(node, type, flags);
        writeEnumCode(type, flags);
        writer.unindent();
        writer.writeln("}");
    }

    private void writeEnumCode(String type, boolean flags) throws IOException {
        writer.writeln("private final " + type + " code;");
        writer.writeln();
        writer.writeln(this.node.getName() + "(" + type + " code) {");
        writer.writeln("    this.code = code;");
        writer.writeln("}");
        writer.writeln();
        writer.writeln("public " + type + " code() {");
        writer.writeln("    return this.code;");
        writer.writeln("}");
        writer.writeln();
        if (flags) {
            writer.writeln("public static EnumSet<" + this.node.getName() + "> from(" + type + " code) {");
            writer.writeln("    EnumSet<" + this.node.getName() + "> set = EnumSet.noneOf(" + this.node.getName() + ".class);");
            writer.writeln("    for (" + this.node.getName() + " e : " + this.node.getName() + ".values()) {");
            writer.writeln("        if ((e.code & code) == e.code) {");
            writer.writeln("            set.add(e);");
            writer.writeln("        }");
            writer.writeln("    }");
            writer.writeln("    return set;");
            writer.writeln("}");
            writer.writeln();
            writer.writeln("public static " + type + " code(EnumSet<" + this.node.getName() + "> flags) {");
            writer.writeln("    " + type + " code = 0;");
            writer.writeln("    for (" + this.node.getName() + " flag : flags) {");
            writer.writeln("        code |= flag.code;");
            writer.writeln("    }");
            writer.writeln("    return code;");
            writer.writeln("}");
        } else {
            writer.writeln("public static " + this.node.getName() + " from(" + type + " code) {");
            writer.writeln("    for (" + this.node.getName() + " e : " + this.node.getName() + ".values()) {");
            writer.writeln("        if (e.code == code) {");
            writer.writeln("            return e;");
            writer.writeln("        }");
            writer.writeln("    }");
            writer.writeln("    return null;");
            writer.writeln("}");
        }
    }

    private void writeEnumProperties(EnumNode node, String type, boolean flags) throws IOException {

        List<PropertyNode> statics = new ArrayList<>();
        for (Node child : (node.getChildNodes())) {
            var prop = (PropertyNode) child;

            if (prop.isEmit()) {
                if (prop.getObsolete() != null) {
                    // including obsolete items can introduce duplicates
                    continue;
//                    writer.writeln("/**")
//                    writer.writeln(" * @deprecated $prop.obsolete")
//                    writer.writeln(" */")
//                    writer.writeln("@Deprecated")
                }

                if (flags && !NUMBER_PATTERN.matcher(getType(prop.getDefaultSymbols().get(0))).matches()) {
                    statics.add(prop);
                } else {
                    List<String> types = prop.getDefaultSymbols().stream().map(symbol -> {
                            var temp = getType(symbol);

                    if (NUMBER_PATTERN.matcher(temp).matches()) {
                        switch (type) {
                            case "long":
                                if (temp.startsWith("-")) {
                                    return temp + "L";
                                }
                                return Long.parseUnsignedLong(temp) + "L";
                            case "byte":
                                return "(byte) " + temp;
                            case "short":
                                return "(short) " + temp;
                            default:
                                if (temp.startsWith("-") || temp.contains("x")) {
                                    return temp;
                                }
                                return String.valueOf(Integer.parseUnsignedInt(temp));
                        }
                    }

                    return temp + ".code";
                    }).collect(Collectors.toList());

                    String val = String.join(" | ", types);
                    writer.writeln(prop.getName() + "(" + val + "),");
                }
            }
        }

        writer.writeln();
        writer.writeln(";");
        writer.writeln();

        for (PropertyNode p : statics) {
            List<String> defaults = p.getDefaultSymbols().stream().map(JavaGen::getType).collect(Collectors.toList());
            writer.writeln("public static final EnumSet<" + this.node.getName() + "> " + p.getName()
                    + " = EnumSet.of(" + String.join(", ", defaults) + ");");
            writer.writeln();
        }
    }

    private static String getType(Symbol symbol) {
        if (symbol instanceof WeakSymbol) {
            var ws = (WeakSymbol) symbol;

            // TODO: 2018-02-21 eeeeeehhh
            if (ws.getIdentifier().contains("CMsgProtoBufHeader")) {
                return "CMsgProtoBufHeader";
            }

            return WEAK_TYPES.containsKey(ws.getIdentifier()) ? WEAK_TYPES.get(ws.getIdentifier()).name : ws.getIdentifier();
        } else if (symbol instanceof StrongSymbol) {
            var ss = (StrongSymbol) symbol;

            if (ss.getProperty() == null) {
                return ss.getClazz().getName();
            } else {
                return ss.getClazz().getName() + "." + ss.getProperty().getName();
            }
        }

        return "INVALID";
    }

    private static int getTypeSize(PropertyNode prop) {
        if ("proto".equals(prop.getFlags())) {
            return 0;
        }

        Symbol sym = prop.getType();

        if (sym instanceof WeakSymbol) {
            var wsym = (WeakSymbol) sym;
            var key = wsym.getIdentifier();

            if (!WEAK_TYPES.containsKey(key)) {
                key = DEFAULT_TYPE;
            }

            if (prop.getFlagsOpt() != null && !prop.getFlagsOpt().isEmpty()) {
                return Integer.parseInt(prop.getFlagsOpt());
            }

            return WEAK_TYPES.get(key).size;
        } else if (sym instanceof StrongSymbol) {
            var ssym = (StrongSymbol) sym;

            if (ssym.getClazz() instanceof EnumNode) {
                var enode = (EnumNode) ssym.getClazz();

                if (enode.getType() instanceof WeakSymbol) {
                    return WEAK_TYPES.get(((WeakSymbol) enode.getType()).getIdentifier()).size;
                } else {
                    return WEAK_TYPES.get(DEFAULT_TYPE).size;
                }
            }
        }

        return 0;
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new StreamProcessingException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                throw new StreamProcessingException(e.getMessage(), e);
            }
        }
    }

    private static class TypeInfo {
        final int size;

        final String name;

        TypeInfo(int size, String name) {
            this.size = size;
            this.name = name;
        }
    }

    private String getPackageName(String suffix) {
        return suffix.startsWith(".") ? this.basePackage + suffix : this.basePackage + "." + suffix;
    }
}
