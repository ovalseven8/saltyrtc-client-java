/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages.c2c;

import org.msgpack.core.MessagePacker;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.Map;

public class Token extends Message {

    public static final String TYPE = "token";

    private byte[] key;

    public Token(byte[] key) {
        this.key = key;
    }

    public Token(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.key = ValidationHelper.validateByteArray(map.get("key"), CryptoProvider.SYMMKEYBYTES, "Key");
    }

    public byte[] getKey() {
        return key;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("key")
                    .packBinaryHeader(this.key.length)
                    .writePayload(this.key);
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
