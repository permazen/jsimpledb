
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;

import org.dellroad.stuff.java.EnumUtil;
import org.jsimpledb.schema.EnumSchemaField;
import org.jsimpledb.schema.SimpleSchemaField;

/**
 * Represents an enum field in a {@link JClass}.
 */
public class JEnumField extends JSimpleField {

    JEnumField(JSimpleDB jdb, String name, int storageId, Class<? extends Enum<?>> enumType,
      boolean indexed, String description, Method getter, Method setter) {
        super(jdb, name, storageId, TypeToken.of(enumType.asSubclass(Enum.class)),
          enumType.getName(), indexed, description, getter, setter);
    }

    @Override
    public <R> R visit(JFieldSwitch<R> target) {
        return target.caseJEnumField(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TypeToken<? extends Enum<?>> getType() {
        return (TypeToken<? extends Enum<?>>)this.typeToken;
    }

    @Override
    EnumSchemaField toSchemaItem(JSimpleDB jdb) {
        final EnumSchemaField schemaField = new EnumSchemaField();
        this.initialize(jdb, schemaField);
        return schemaField;
    }

    @SuppressWarnings("unchecked")
    void initialize(JSimpleDB jdb, SimpleSchemaField schemaField0) {
        super.initialize(jdb, schemaField0);
        final EnumSchemaField schemaField = (EnumSchemaField)schemaField0;
        schemaField.getIdentifiers().clear();
        for (Enum<?> value : EnumUtil.getValues((Class<Enum<?>>)this.getType().getRawType()))
            schemaField.getIdentifiers().add(value.name());
    }

    @Override
    JEnumFieldInfo toJFieldInfo(int parentStorageId) {
        return new JEnumFieldInfo(this, parentStorageId);
    }
}
