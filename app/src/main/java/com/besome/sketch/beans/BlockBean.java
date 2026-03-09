package com.besome.sketch.beans;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

import pro.sketchware.core.ClassInfo;
import pro.sketchware.core.ComponentTypeMapper;

/**
 * Represents a single block in the logic editor.
 * <p>
 * Each block has an {@link #opCode} that identifies its function, a {@link #spec} string
 * that defines its visual layout and parameter placeholders (e.g. {@code "%s"}, {@code "%d"},
 * {@code "%m.view"}), a {@link #type} character indicating its shape ({@code " "} for
 * statement, {@code "b"} for boolean, {@code "d"} for number, {@code "s"} for string),
 * and a list of {@link #parameters} containing literal values or block references
 * (prefixed with {@code "@"}).
 * <p>
 * Blocks form linked chains via {@link #nextBlock} (the next statement) and can contain
 * nested sub-stacks via {@link #subStack1} and {@link #subStack2} (for if/else, repeat, etc.).
 *
 * @see pro.sketchware.core.BlockInterpreter
 * @see pro.sketchware.core.BlockCodeRegistry
 */
public class BlockBean extends SelectableBean implements Parcelable {
    public static final Parcelable.Creator<BlockBean> CREATOR = new Parcelable.Creator<>() {
        @Override
        public BlockBean createFromParcel(Parcel source) {
            return new BlockBean(source);
        }

        @Override
        public BlockBean[] newArray(int size) {
            return new BlockBean[size];
        }
    };

    public ClassInfo classInfo;
    @Expose
    public int color;
    @Expose
    public String id;
    @Expose
    public int nextBlock;
    @Expose
    public String opCode;
    public ArrayList<ClassInfo> parameterClassInfo;
    @Expose
    public ArrayList<String> parameters;
    @Expose
    public String spec;
    @Expose
    public int subStack1;
    @Expose
    public int subStack2;
    @Expose
    public String type;
    @Expose
    public String typeName;
    @Expose
    public boolean disabled;
    @Expose
    public boolean collapsed;
    @Expose
    public boolean collapsed2;

    public BlockBean() {
        parameters = new ArrayList<>();
        subStack1 = -1;
        subStack2 = -1;
        nextBlock = -1;
    }

    /**
     * Constructor without <code>typeName</code>.
     */
    public BlockBean(String id, String spec, String type, String opCode) {
        this(id, spec, type, "", opCode);
    }

    /**
     * Constructs a block with full parameters.
     *
     * @param id       the unique block ID within an event chain (e.g. {@code "1"}, {@code "5"})
     * @param spec     the display spec with parameter placeholders
     * @param type     the block shape character ({@code " "}, {@code "b"}, {@code "d"}, {@code "s"})
     * @param typeName the component type name (e.g. {@code "textview"}, {@code "timer"})
     * @param opCode   the operation code identifying this block's function
     */
    public BlockBean(String id, String spec, String type, String typeName, String opCode) {
        this.id = id;
        this.spec = spec;
        this.type = type;
        this.typeName = typeName;
        this.opCode = opCode;
        parameters = new ArrayList<>();
        subStack1 = -1;
        subStack2 = -1;
        nextBlock = -1;
        buildClassInfo();
    }

    public BlockBean(Parcel parcel) {
        id = parcel.readString();
        spec = parcel.readString();
        type = parcel.readString();
        typeName = parcel.readString();
        opCode = parcel.readString();
        color = parcel.readInt();
        parameters = (ArrayList<String>) parcel.readSerializable();
        subStack1 = parcel.readInt();
        subStack2 = parcel.readInt();
        nextBlock = parcel.readInt();
        disabled = parcel.readByte() != 0;
        collapsed = parcel.readByte() != 0;
        collapsed2 = parcel.readByte() != 0;
        buildClassInfo();
    }

    public static Parcelable.Creator<BlockBean> getCreator() {
        return CREATOR;
    }

    private void buildClassInfo() {
        classInfo = ComponentTypeMapper.getClassInfo(type, typeName);
        parameterClassInfo = ComponentTypeMapper.getParamClassInfoList(spec);
    }

    /**
     * Deep-copies all fields from another BlockBean into this one,
     * including rebuilding class info from the copied type/spec.
     *
     * @param other the source block to copy from
     */
    public void copy(BlockBean other) {
        id = other.id;
        spec = other.spec;
        type = other.type;
        typeName = other.typeName;
        opCode = other.opCode;
        color = other.color;
        parameters = new ArrayList<>(other.parameters);
        subStack1 = other.subStack1;
        subStack2 = other.subStack2;
        nextBlock = other.nextBlock;
        disabled = other.disabled;
        collapsed = other.collapsed;
        collapsed2 = other.collapsed2;
        buildClassInfo();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns the {@link ClassInfo} for this block's return type.
     * Lazily builds class info on first access.
     *
     * @return the class info, never null
     */
    public ClassInfo getClassInfo() {
        if (classInfo == null) {
            buildClassInfo();
        }
        return classInfo;
    }

    /**
     * Returns the list of {@link ClassInfo} objects for each parameter slot,
     * derived from the spec's parameter placeholders.
     *
     * @return the parameter class info list, never null
     */
    public ArrayList<ClassInfo> getParamClassInfo() {
        if (parameterClassInfo == null) {
            buildClassInfo();
        }
        return parameterClassInfo;
    }

    /**
     * Checks value equality with another BlockBean by comparing all fields
     * including parameters. Used by {@link pro.sketchware.core.BlockHistoryManager}
     * to detect actual changes before recording undo history.
     *
     * @param other the block to compare with
     * @return {@code true} if all fields are equal
     */
    public boolean isEqual(BlockBean other) {
        if (other == null) {
            return false;
        }
        String id = this.id;
        if (!(id == null || id.equals(other.id))) {
            return false;
        }
        String spec = this.spec;
        if (!((spec == null || spec.equals(other.spec)) && type.equals(other.type))) {
            return false;
        }
        String typeName = this.typeName;
        if (!((typeName == null || typeName.equals(other.typeName)) && opCode.equals(other.opCode)
                && color == other.color && subStack1 == other.subStack1
                && subStack2 == other.subStack2 && nextBlock == other.nextBlock
                && disabled == other.disabled
                && collapsed == other.collapsed
                && collapsed2 == other.collapsed2)) {
            return false;
        }
        ArrayList<String> parameters = this.parameters;
        if (!(parameters == null || parameters.size() == other.parameters.size())) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            String thisParam = parameters.get(i);
            String otherParam = other.parameters.get(i);
            if (!(thisParam == null || thisParam.equals(otherParam))) {
                return false;
            }
        }
        return true;
    }

    public void print() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(spec);
        dest.writeString(type);
        dest.writeString(typeName);
        dest.writeString(opCode);
        dest.writeInt(color);
        dest.writeSerializable(parameters);
        dest.writeInt(subStack1);
        dest.writeInt(subStack2);
        dest.writeInt(nextBlock);
        dest.writeByte((byte) (disabled ? 1 : 0));
        dest.writeByte((byte) (collapsed ? 1 : 0));
        dest.writeByte((byte) (collapsed2 ? 1 : 0));
    }

    @Override
    @NonNull
    public BlockBean clone() {
        BlockBean blockBean = new BlockBean();
        blockBean.copy(this);
        return blockBean;
    }
}
