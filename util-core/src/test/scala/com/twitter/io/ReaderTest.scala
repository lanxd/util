package com.twitter.io

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.util.{Await, Awaitable, Future, Promise}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.atomic.AtomicBoolean
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class ReaderTest
    extends FunSuite
    with GeneratorDrivenPropertyChecks
    with Matchers
    with Eventually
    with IntegrationPatience {

  private def arr(i: Int, j: Int) = Array.range(i, j).map(_.toByte)
  private def buf(i: Int, j: Int) = Buf.ByteArray.Owned(arr(i, j))

  private def await[T](t: Awaitable[T]): T = Await.result(t, 5.seconds)

  private def toSeq(b: Option[Buf]): Seq[Byte] = b match {
    case None => fail("Expected full buffer")
    case Some(buf) =>
      val a = new Array[Byte](buf.length)
      buf.write(a, 0)
      a.toSeq
  }

  def undefined: AsyncStream[Reader[Buf]] = throw new Exception

  private def assertReadWhileReading(r: Reader[Buf]): Unit = {
    val f = r.read(1)
    intercept[IllegalStateException] { await(r.read(1)) }
    assert(!f.isDefined)
  }

  private def assertFailed(r: Reader[Buf], p: Promise[Option[Buf]]): Unit = {
    val f = r.read(1)
    assert(!f.isDefined)
    p.setException(new Exception)
    intercept[Exception] { await(f) }
    intercept[Exception] { await(r.read(0)) }
    intercept[Exception] { await(r.read(1)) }
  }

  test("Reader.copy - source and destination equality") {
    forAll { (p: Array[Byte], q: Array[Byte], r: Array[Byte]) =>
      val rw = new Pipe[Buf]
      val bos = new ByteArrayOutputStream

      val w = Writer.fromOutputStream(bos, 31)
      val f = Reader.copy(rw, w).ensure(w.close())
      val g =
        rw.write(Buf.ByteArray.Owned(p)).before {
          rw.write(Buf.ByteArray.Owned(q)).before {
            rw.write(Buf.ByteArray.Owned(r)).before { rw.close() }
          }
        }

      await(Future.join(f, g))

      val b = new ByteArrayOutputStream
      b.write(p)
      b.write(q)
      b.write(r)
      b.flush()

      bos.toByteArray should equal(b.toByteArray)
    }
  }

  test("Reader.concat") {
    forAll { (ss: List[String]) =>
      val readers = ss.map { s =>
        BufReader(Buf.Utf8(s))
      }
      val buf = Reader.readAll(Reader.concat(AsyncStream.fromSeq(readers)))
      await(buf) should equal(Buf.Utf8(ss.mkString))
    }
  }

  test("Reader.concat - discard") {
    val p = new Promise[Option[Buf]]
    val head = new Reader[Buf] {
      def read(n: Int): Future[Option[Buf]] = p
      def discard(): Unit = p.setException(new Reader.ReaderDiscarded)
    }
    val reader = Reader.concat(head +:: undefined)
    reader.discard()
    assert(p.isDefined)
  }

  test("Reader.concat - read while reading") {
    val p = new Promise[Option[Buf]]
    val head = new Reader[Buf] {
      def read(n: Int): Future[Option[Buf]] = p
      def discard(): Unit = p.setException(new Reader.ReaderDiscarded)
    }
    val reader = Reader.concat(head +:: undefined)
    assertReadWhileReading(reader)
  }

  test("Reader.concat - failed") {
    val p = new Promise[Option[Buf]]
    val head = new Reader[Buf] {
      def read(n: Int): Future[Option[Buf]] = p
      def discard(): Unit = p.setException(new Reader.ReaderDiscarded)
    }
    val reader = Reader.concat(head +:: undefined)
    assertFailed(reader, p)
  }

  test("Reader.concat - lazy tail") {
    val head = new Reader[Buf] {
      def read(n: Int): Future[Option[Buf]] = Future.exception(new Exception)
      def discard(): Unit = ()
    }
    val p = new Promise[Unit]
    def tail: AsyncStream[Reader[Buf]] = {
      p.setDone()
      AsyncStream.empty
    }
    val combined = Reader.concat(head +:: tail)
    val buf = Reader.readAll(combined)
    intercept[Exception] { await(buf) }
    assert(!p.isDefined)
  }

  test("Reader.fromStream closes resources on EOF read") {
    val in = spy(new ByteArrayInputStream(arr(0, 10)))
    val r = Reader.fromStream(in)
    val f = Reader.readAll(r)
    assert(await(f) == buf(0, 10))
    eventually {
      verify(in).close()
    }
  }

  test("Reader.fromStream closes resources on discard") {
    val in = spy(new ByteArrayInputStream(arr(0, 10)))
    val r = Reader.fromStream(in)
    r.discard()
    eventually {
      verify(in).close()
    }
  }

  test("Reader.fromAsyncStream completes when stream is empty") {
    val as = AsyncStream(buf(1, 10))
    val r = Reader.fromAsyncStream(as)
    val f = Reader.readAll(r)
    assert(await(f) == buf(1, 10))
  }

  test("Reader.fromAsyncStream fails on exceptional stream") {
    val as = AsyncStream.exception(new Exception())
    val r = Reader.fromAsyncStream(as)
    val f = Reader.readAll(r)
    intercept[Exception] { await(f) }
  }

  test("Reader.fromAsyncStream only evaluates tail when buffer is exhausted") {
    val tailEvaluated = new AtomicBoolean(false)
    def tail: AsyncStream[Buf] = {
      tailEvaluated.set(true)
      AsyncStream.empty
    }
    val as = AsyncStream.mk(buf(0, 10), tail)
    val r = Reader.fromAsyncStream(as)

    // partially read the buffer
    await(r.read(9))
    assert(!tailEvaluated.get())

    // read the rest of the buffer
    await(r.read(2))
    assert(tailEvaluated.get())
  }

}
