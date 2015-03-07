
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.parse;

import org.jsimpledb.core.DeletedObjectException;
import org.jsimpledb.core.ObjId;
import org.jsimpledb.core.ObjType;
import org.jsimpledb.core.SchemaVersion;
import org.jsimpledb.core.Transaction;

/**
 * Utility class holding meta-data about a database object.
 */
public class ObjInfo {

    private final ObjId id;
    private final SchemaVersion schemaVersion;
    private final ObjType type;

    /**
     * Constructor.
     *
     * @param tx database transaction
     * @param id the ID of the object to query
     * @throws DeletedObjectException if object does not exist
     */
    public ObjInfo(Transaction tx, ObjId id) {
        this.id = id;
        this.schemaVersion = tx.getSchema().getVersion(tx.getSchemaVersion(id));
        this.type = this.schemaVersion.getObjType(id.getStorageId());
    }

    public ObjId getObjId() {
        return this.id;
    }

    public SchemaVersion getSchemaVersion() {
        return this.schemaVersion;
    }

    public ObjType getObjType() {
        return this.type;
    }

    @Override
    public String toString() {
        return this.id + " type " + this.type.getName() + "#" + this.type.getStorageId()
          + " version " + this.schemaVersion.getVersionNumber();
    }

    /**
     * Get object meta-data.
     *
     * @param session parse session
     * @param id the ID of the object to query
     * @return object info, or null if object doesn't exist
     */
    public static ObjInfo getObjInfo(ParseSession session, ObjId id) {
        try {
            return new ObjInfo(session.getTransaction(), id);
        } catch (DeletedObjectException e) {
            return null;
        }
    }
}

