import java.math.BigInteger
import java.util.concurrent.TimeUnit

import org.bson.codecs.configuration.CodecRegistries
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Person {
  def apply(firstName: String, lastName: String, isoNumber: BigInteger, nastyList: List[Long]): Person =
    Person(new ObjectId(), firstName, lastName, isoNumber, nastyList)
}

case class Person(_id: ObjectId, firstName: String, lastName: String, isoNumber: BigInteger, nastyList: List[Long])

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class BigIntegerCodec extends Codec[BigInteger] {
  override def encode(writer: BsonWriter, value: BigInteger, encoderContext: EncoderContext): Unit = {
    writer.writeString(value.toString(10))
  }

  override def decode(reader: BsonReader, decoderContext: DecoderContext): BigInteger = new BigInteger(reader.readString())

  override def getEncoderClass: Class[BigInteger] = classOf[BigInteger]
}

import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}


object DBPusher {
  def main(args: Array[String]): Unit = {
    val mongoClient: MongoClient = MongoClient()
    val codecRegistry = fromRegistries(fromProviders(classOf[Person]), CodecRegistries.fromCodecs(new BigIntegerCodec()), DEFAULT_CODEC_REGISTRY )

    val database: MongoDatabase = mongoClient.getDatabase("mydb").withCodecRegistry(codecRegistry)
    val collection: MongoCollection[Person] = database.getCollection("test")

    val person: Person = Person("Ada", "Lovelace", new BigInteger("10"), List(1L,2L,3L))
    println(Await.result(collection.insertOne(person).toFuture(), Duration(10, TimeUnit.SECONDS)))
  }
}
