package com.spredfast.kafka.connect.s3;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Configurable;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

/**
 * Encodes raw bytes, prefixed by a 4 byte, big-endian integer
 * indicating the length of the byte sequence.
 */
public class ByteLengthFormat implements S3RecordFormat, Configurable {

  private static final int LEN_SIZE = 4;
  private static final byte[] NO_BYTES = {};

  private boolean includesKeys;

  public ByteLengthFormat() {
  }

  public ByteLengthFormat(boolean includesKeys) {
    this.includesKeys = includesKeys;
  }

  @Override
  public void configure(Map<String, ?> configs) {
    includesKeys = Optional.ofNullable(configs.get("include.keys")).map(Object::toString)
      .map(Boolean::valueOf).orElse(true);
  }

  @Override
  public S3RecordsWriter newWriter() {
    return records -> records.map(this::encode);
  }

  private byte[] encode(ProducerRecord<byte[], byte[]> r) {
    // write optionally the key, and the value, each preceded by their length
    byte[] key = includesKeys ? Optional.ofNullable(r.key()).orElse(NO_BYTES) : NO_BYTES;
    byte[] value = Optional.ofNullable(r.value()).orElse(NO_BYTES);
    byte[] result = new byte[LEN_SIZE + value.length + (includesKeys ? key.length + LEN_SIZE : 0)];
    ByteBuffer wrapped = ByteBuffer.wrap(result);
    if (includesKeys) {
      wrapped.putInt(key.length);
      wrapped.put(key);
    }
    wrapped.putInt(value.length);
    wrapped.put(value);
    return result;
  }

  @Override
  public S3RecordsReader newReader() {
    return new BytesRecordReader(includesKeys);
  }
}
