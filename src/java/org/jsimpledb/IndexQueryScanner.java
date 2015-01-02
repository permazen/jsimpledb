
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.jsimpledb.annotation.IndexQuery;
import org.jsimpledb.util.AnnotationScanner;

/**
 * Scans for {@link IndexQuery &#64;IndexQuery} annotations.
 */
class IndexQueryScanner<T> extends AnnotationScanner<T, IndexQuery> {

    IndexQueryScanner(JClass<T> jclass) {
        super(jclass, IndexQuery.class);
    }

    @Override
    protected boolean includeMethod(Method method, IndexQuery annotation) {
        this.checkNotStatic(method);
        this.checkParameterTypes(method);
        return true;                                    // we check return type in IndexMethodInfo
    }

    @Override
    protected IndexMethodInfo createMethodInfo(Method method, IndexQuery annotation) {
        return new IndexMethodInfo(method, annotation);
    }

// IndexMethodInfo

    class IndexMethodInfo extends MethodInfo {

        final IndexInfo indexInfo;
        final int queryType;

        @SuppressWarnings("unchecked")
        IndexMethodInfo(Method method, IndexQuery annotation) {
            super(method, annotation);

            // Get index info
            try {
                this.indexInfo = new IndexInfo(IndexQueryScanner.this.jclass.jdb,
                  annotation.type() != void.class ? annotation.type() : method.getDeclaringClass(), annotation.value());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(IndexQueryScanner.this.getErrorPrefix(method) + e.getMessage(), e);
            }

            // Check method's return type
            IndexQueryScanner.this.checkReturnType(method, this.indexInfo.indexReturnTypes);

            // Determine the query type (normal object query or some kind of index entry query) from method return type
            final TypeToken<?> queryObjectType = Util.getTypeParameter(
              Util.getTypeParameter(TypeToken.of(method.getGenericReturnType()), 1), 0);
            this.queryType = this.indexInfo.targetSuperFieldInfo != null ?
              this.indexInfo.targetSuperFieldInfo.getIndexEntryQueryType(queryObjectType) : 0;
        }
    }

// IndexInfo

    static class IndexInfo {

        final TypeToken<?> type;
        final TypeToken<?> targetType;
        final JSimpleFieldInfo targetFieldInfo;
        final TypeToken<?> targetFieldType;
        final JComplexFieldInfo targetSuperFieldInfo;
        final ArrayList<TypeToken<?>> indexReturnTypes = new ArrayList<TypeToken<?>>();

        IndexInfo(JSimpleDB jdb, Class<?> type, String fieldName) {

            // Sanity check
            if (jdb == null)
                throw new IllegalArgumentException("null jdb");
            if (type == null)
                throw new IllegalArgumentException("null type");
            if (fieldName == null)
                throw new IllegalArgumentException("null fieldName");

            // Get start type
            if (type.isPrimitive() || type.isArray())
                throw new IllegalArgumentException("invalid type " + type);
            this.type = Util.getWildcardedType(type);

            // Parse reference path
            final ReferencePath path = jdb.parseReferencePath(this.type, fieldName, true);
            if (path.getReferenceFields().length > 0)
                throw new IllegalArgumentException("invalid field name `" + fieldName + "': contains intermediate reference(s)");

            // Verify target field is simple
            if (!(path.targetFieldInfo instanceof JSimpleFieldInfo))
                throw new IllegalArgumentException(path.targetFieldInfo + " does not support indexing; it is not a simple field");

            // Get target object, field, and complex super-field (if any)
            this.targetType = path.targetType;
            this.targetFieldInfo = (JSimpleFieldInfo)path.targetFieldInfo;
            this.targetFieldType = path.targetFieldType;
            this.targetSuperFieldInfo = path.targetSuperFieldInfo;

            // Verify the field is actually indexed
            if (!this.targetFieldInfo.isIndexed())
                throw new IllegalArgumentException(this.targetFieldInfo + " is not indexed");

            // Get valid index return types for this field
            try {
                this.targetFieldInfo.addIndexReturnTypes(this.indexReturnTypes, this.type, this.targetFieldType);
            } catch (UnsupportedOperationException e) {
                throw new IllegalArgumentException("indexing is not supported for " + this.targetFieldInfo, e);
            }

            // If field is a complex sub-field, determine add complex index entry type(s)
            if (this.targetSuperFieldInfo != null) {
                this.targetSuperFieldInfo.addIndexEntryReturnTypes(this.indexReturnTypes,
                  this.type, this.targetFieldInfo, this.targetFieldType);
            }
        }
    }
}
