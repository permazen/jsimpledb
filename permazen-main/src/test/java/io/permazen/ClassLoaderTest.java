
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package io.permazen;

import io.permazen.annotation.PermazenType;
import io.permazen.core.ObjId;
import io.permazen.test.TestSupport;

import org.testng.annotations.Test;

public class ClassLoaderTest extends TestSupport {

/*

    This test tickles a bug that was caused a LinkageError with "attempted  duplicate class definition"
    in our ClassLoader's findClass() method.

    The bug is tickled by loading the snapshot class, which triggers an implicit loading of the normal
    class (which is its superclass), then attempting to load the normal class explicitly.

    The bug was caused by our ClassLoader accepting and defining classes with names having slashes
    instead of dots.

*/
    @Test
    public void testClassLoaderTest() {
        final Permazen jdb = BasicTest.getPermazen(Person.class);
        JTransaction jtx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(jtx);
        try {
            final SnapshotJTransaction stx = jtx.getSnapshotTransaction();
            final ObjId id = new ObjId("0100000000000000");
            stx.get(id).getObjId();                                     // causes snapshot class to load
            jtx.get(id).getObjId();                                     // causes normal class to load redundantly
            jtx.commit();
        } finally {
            JTransaction.setCurrent(null);
        }
    }

// Model Classes

    @PermazenType(storageId = 1)
    public abstract static class Person implements JObject {
    }
}

