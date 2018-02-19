/**
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.garyaiki.dendrites

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{Decoder, DecoderFactory, Encoder, EncoderFactory}
import org.apache.avro.specific.SpecificDatumWriter
import scala.io.Source
import scala.io.Source.fromInputStream

/** Avro serializer/deserializer functions
  *
  * Load Avro Schema from file
  * {{{
  * val schema = loadSchema(filename)
  * }}}
  * Serialize case class to bytearray
  * {{{
  * val bytes = ccToByteArray(schema, GetAccountBalances(1L))
  * val record = new ProducerRecord[String, Array[Byte]](topic, key, bytes)
  * val rm: RecordMetadata = producer.send(record).get()
  * }}}
  * Map bytearray to Avro GenericRecord
  * {{{
  * new GraphStageLogic(shape) {
  * setHandler(in, new InHandler {
  *   override def onPush(): Unit = {
  *     val bytes = grab(in)
  *     val record = byteArrayToGenericRecord(schema, bytes)
  *     push(out, f(record))
  *   }
  * })
  * }}}
  */
package object avro {

  /** Load Avro Schema from file
    *
    * @param filename must be in classpath
    * @param filepath, '/' for default src/main/resources
    * @return Schema
    */
  def loadSchema(filename: String, path: String = "/"): Schema = {
    val schemaStream = fromInputStream(getClass.getResourceAsStream(path + filename))
    val schemaStr = schemaStream.mkString
    new Schema.Parser().parse(schemaStr)
  }

  /** create byte array from schema and case class
    *
    * @tparam A case class or tuple
    * @param schema for type A
    * @param cc case class or tuple that only has simple types
    * @return byte array of values
    */
  def ccToByteArray[A <: Product](schema: Schema, cc: A): Array[Byte] = {
    val record = new GenericData.Record(schema)
    val writer = new GenericDatumWriter[GenericRecord](schema)
    ccToGenericRecord(record)(cc)
    toByteArray(writer)(record)
  }

  /** Map case class values that are only simple types to GenericRecord
    *
    * @tparam A case class or tuple
    * @param GenericData.Record initialized with schema for case class
    * @param cc a case class (or tuple)
    */
  def ccToGenericRecord[A <: Product](gRecord: GenericData.Record)(cc: A): Unit = {
    val kvMap = ccToMap(cc)
    kvMap foreach { case (key, value) => gRecord.put(key, value)}
  }

  /** Create encoded ByteArray from GenericDatumWriter and GenericRecord
    *
    * You can pass writer once and curry, then pass each record to curried function
    * @param writer for the Avro schema for a case class
    * @param genericRecord Avro GenericRecord containing case class data
    * @return byteArray for Kafka ByteArraySerializer
    */
  def toByteArray(writer: GenericDatumWriter[GenericRecord])(genericRecord: GenericRecord): Array[Byte] = {
    val os = new ByteArrayOutputStream()
    try {
      val encoder = EncoderFactory.get.binaryEncoder(os, null)
      writer.write(genericRecord, encoder)
      encoder.flush()
      os.toByteArray
    } finally {
      os.close()
    }
  }

  /** Map a bytearray to an Avro GenericRecord
    *
    * @param schema for case class
    * @param bytes to deserialize
    * @return GenericRecord
    */
  def byteArrayToGenericRecord(schema: Schema, bytes: Array[Byte]): GenericRecord = {
    val reader = new GenericDatumReader[GenericRecord](schema)
    val in = new ByteArrayInputStream(bytes)
    val decoder = DecoderFactory.get.binaryDecoder(in, null)
    reader.read(null, decoder)
  }
}
