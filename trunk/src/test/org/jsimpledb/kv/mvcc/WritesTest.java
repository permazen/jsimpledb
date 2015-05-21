
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.kv.mvcc;

import com.google.common.base.Converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.NavigableMap;

import org.jsimpledb.TestSupport;
import org.jsimpledb.kv.KeyRange;
import org.jsimpledb.kv.KeyRanges;
import org.jsimpledb.kv.util.NavigableMapKVStore;
import org.jsimpledb.util.ByteUtil;
import org.jsimpledb.util.ConvertedNavigableMap;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class WritesTest extends TestSupport {

    @Test(dataProvider = "writes")
    public void testSerializeWrites(Writes writes) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writes.serialize(output);
        final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        final Writes writes2 = Writes.deserialize(input);
        Assert.assertEquals(writes2.getRemoves(), writes.getRemoves());
        Assert.assertEquals(writes2.getPuts(), writes.getPuts());
        Assert.assertEquals(writes2.getAdjusts(), writes.getAdjusts());
        final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        writes2.serialize(output2);
        Assert.assertEquals(output2.toByteArray(), output.toByteArray());
    }

    @DataProvider(name = "writes")
    private Writes[][] genWrites() throws Exception {
        final ArrayList<Writes> list = new ArrayList<>();

        final Writes writes1 = new Writes();
        list.add(writes1);

        final Writes writes2 = new Writes();
        writes2.setRemoves(writes2.getRemoves().add(KeyRange.forPrefix(b("6666"))));
        writes2.setRemoves(writes2.getRemoves().add(new KeyRange(b("003333"), b("004444"))));
        list.add(writes2);

        final Writes writes3 = new Writes();
        writes3.getPuts().put(b("1234"), b("5678"));
        writes3.getPuts().put(b("3333"), b("4444"));
        list.add(writes3);

        final Writes writes4 = new Writes();
        writes4.getAdjusts().put(b("77777777"), 1234567890L);
        writes4.getAdjusts().put(b("99999999"), Long.MIN_VALUE);
        list.add(writes4);

        final Writes[][] array = new Writes[list.size()][];
        for (int i = 0; i < list.size(); i++)
            array[i] = new Writes[] { list.get(i) };
        return array;
    }

    @Test
    public void testMutations() throws Exception {

        // Create store
        final NavigableMapKVStore beforeMutations = new NavigableMapKVStore();
        beforeMutations.put(b("01"), b("1234"));
        beforeMutations.put(b("02"), b("5555"));
        beforeMutations.put(b("33"), b("38"));
        beforeMutations.put(b("3311"), b("33"));
        beforeMutations.put(b("331100"), b(""));
        beforeMutations.put(b("40"), b("40"));
        beforeMutations.put(b("33110000"), b("23"));
        beforeMutations.put(b("550001"), b("0001"));
        beforeMutations.put(b("550002"), b("0002"));
        beforeMutations.put(b("55000200"), b("000200"));
        beforeMutations.put(b("550003"), b("0003"));
        beforeMutations.put(b("550001"), b("0004"));
        beforeMutations.put(b("62"), beforeMutations.encodeCounter(12345));
        beforeMutations.put(b("66"), b("99"));

        // Set up mutations
        final Writes writes = new Writes();
        writes.setRemoves(
          KeyRanges.forPrefix(b("3311"))
            .add(new KeyRange(b("66"), null))
            .add(new KeyRange(b("550002"), b("550003")))
            .add(new KeyRange(b(""), b("02")))
            .add(new KeyRange(b("40")))
            .add(new KeyRange(b("4444"), b("5500"))));
        writes.getPuts().put(b("331100"), b("22"));
        writes.getPuts().put(b("7323"), b("9933"));
        writes.getPuts().put(b(""), b("ffff"));
        writes.getPuts().put(b("6622"), b("22"));
        writes.getPuts().put(b("45"), b("45"));
        writes.getAdjusts().put(b("62"), -19L);

        // Get expected result
        final NavigableMapKVStore afterMutations = new NavigableMapKVStore();

        // After removes...
        afterMutations.put(b("02"), b("5555"));
        afterMutations.put(b("33"), b("38"));
        afterMutations.put(b("550001"), b("0001"));
        afterMutations.put(b("550003"), b("0003"));
        afterMutations.put(b("550001"), b("0004"));

        // After puts...
        afterMutations.put(b("331100"), b("22"));
        afterMutations.put(b("7323"), b("9933"));
        afterMutations.put(b(""), b("ffff"));
        afterMutations.put(b("6622"), b("22"));
        afterMutations.put(b("45"), b("45"));

        // After adjusts...
        afterMutations.put(b("62"), afterMutations.encodeCounter(12345L - 19L));

        // Apply mutations and check result
        final NavigableMapKVStore kv1 = new NavigableMapKVStore();
        kv1.getNavigableMap().putAll(beforeMutations.getNavigableMap());
        writes.applyTo(kv1);
        this.compare(kv1, afterMutations);

        // Go through a (de)serialization cycle and repeat
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writes.serialize(output);
        final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        final Writes writes2 = Writes.deserialize(input);
        final NavigableMapKVStore kv2 = new NavigableMapKVStore();
        kv2.getNavigableMap().putAll(beforeMutations.getNavigableMap());
        writes2.applyTo(kv2);
        this.compare(kv2, afterMutations);
    }

    private void compare(NavigableMapKVStore actual, NavigableMapKVStore expected) {
        final NavigableMap<String, String> actualView = this.stringView(actual.getNavigableMap());
        final NavigableMap<String, String> expectedView = this.stringView(expected.getNavigableMap());
        Assert.assertEquals(actualView, expectedView,
          "ACTUAL:\n  " + actualView + "\nEXPECTED:\n  " + expectedView + "\n");
    }

    private NavigableMap<String, String> stringView(NavigableMap<byte[], byte[]> byteMap) {
        if (byteMap == null)
            return null;
        final Converter<String, byte[]> converter = ByteUtil.STRING_CONVERTER.reverse();
        return new ConvertedNavigableMap<String, String, byte[], byte[]>(byteMap, converter, converter);
    }
}

