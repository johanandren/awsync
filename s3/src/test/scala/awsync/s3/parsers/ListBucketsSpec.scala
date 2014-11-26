package awsync.s3.parsers

import awsync.s3.BucketName
import awsync.s3.AbstractSpec

class ListBucketsSpec extends AbstractSpec {

  describe("ListBucketsParser") {

    it("Parses xml into a seq of buckets") {
      val xml = <ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
        <Owner>
          <ID>4e5cc17d1aa88703483faeb4d7a80c0bc2382e4f657506849db9e9d4543e61f41</ID>
          <DisplayName>nobody</DisplayName>
        </Owner>
        <Buckets>
          <Bucket>
            <Name>sample</Name>
            <CreationDate>2013-09-30T11:17:16.000Z</CreationDate>
          </Bucket>
        </Buckets>
      </ListAllMyBucketsResult>

      val result = ListBuckets.parse(xml)
      result.isSuccess should be (true)
      result.get.map(_._1) should be (Seq(BucketName("sample")))
    }

  }

}