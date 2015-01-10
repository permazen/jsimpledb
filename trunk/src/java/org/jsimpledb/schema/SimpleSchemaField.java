
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.schema;

import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jsimpledb.core.FieldType;
import org.jsimpledb.core.InvalidSchemaException;

/**
 * A simple field in a {@link SchemaObjectType}.
 */
public class SimpleSchemaField extends SchemaField {

    private String type;
    private boolean indexed;

    /**
     * Get the name of this field's type. For example {@code "int"} for primitive integer type,
     * {@code "java.util.Date"} for the built-in {@link java.util.Date} type, any custom type name, etc.
     */
    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get whether this field is indexed or not.
     */
    public boolean isIndexed() {
        return this.indexed;
    }
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    @Override
    void validate() {
        super.validate();
        if (this.type == null)
            throw new InvalidSchemaException("invalid " + this + ": no type specified");
        if (!Pattern.compile(FieldType.NAME_PATTERN).matcher(this.type).matches()) {
            throw new InvalidSchemaException("invalid " + super.toString() + " type `" + this.type
              + "': does not match pattern \"" + FieldType.NAME_PATTERN + "\"");
        }
    }

    @Override
    public <R> R visit(SchemaFieldSwitch<R> target) {
        return target.caseSimpleSchemaField(this);
    }

    @Override
    boolean isCompatibleWithInternal(AbstractSchemaItem that0) {
        final SimpleSchemaField that = (SimpleSchemaField)that0;
        if (!this.type.equals(that.type))
            return false;
        if (this.indexed != that.indexed)
            return false;
        return true;
    }

// XML Reading

    @Override
    void readAttributes(XMLStreamReader reader, int formatVersion) throws XMLStreamException {
        super.readAttributes(reader, formatVersion);
        final String typeAttr = this.getAttr(reader, TYPE_ATTRIBUTE, false);
        if (typeAttr != null)
            this.setType(typeAttr);
        final Boolean indexedAttr = this.getBooleanAttr(reader, INDEXED_ATTRIBUTE, false);
        if (indexedAttr != null)
            this.setIndexed(indexedAttr);
    }

    @Override
    final void writeXML(XMLStreamWriter writer) throws XMLStreamException {
        this.writeXML(writer, true);
    }

// XML Writing

    void writeXML(XMLStreamWriter writer, boolean includeName) throws XMLStreamException {
        writer.writeEmptyElement(SIMPLE_FIELD_TAG.getNamespaceURI(), SIMPLE_FIELD_TAG.getLocalPart());
        this.writeAttributes(writer, includeName);
    }

    @Override
    final void writeAttributes(XMLStreamWriter writer, boolean includeName) throws XMLStreamException {
        super.writeAttributes(writer, includeName);
        this.writeSimpleAttributes(writer);
    }

    void writeSimpleAttributes(XMLStreamWriter writer) throws XMLStreamException {
        if (this.type != null)
            writer.writeAttribute(TYPE_ATTRIBUTE.getNamespaceURI(), TYPE_ATTRIBUTE.getLocalPart(), this.type);
        if (this.indexed)
            writer.writeAttribute(INDEXED_ATTRIBUTE.getNamespaceURI(), INDEXED_ATTRIBUTE.getLocalPart(), "" + this.indexed);
    }

// Object

    @Override
    public String toString() {
        return super.toString() + (this.type != null ? " of type " + this.type : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!super.equals(obj))
            return false;
        final SimpleSchemaField that = (SimpleSchemaField)obj;
        return (this.type != null ? this.type.equals(that.type) : that.type == null) && this.indexed == that.indexed;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.type != null ? this.type.hashCode() : 0) ^ (this.indexed ? 1 : 0);
    }

// Cloneable

    @Override
    public SimpleSchemaField clone() {
        return (SimpleSchemaField)super.clone();
    }
}
