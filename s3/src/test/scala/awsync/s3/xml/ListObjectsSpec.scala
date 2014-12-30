package awsync.s3.xml

import awsync.s3._

class ListObjectsSpec extends AbstractSpec {

  describe("ListObjectsParser") {

    it("parses a chunk of xml into a list of object details") {
      val xml = <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
        <Name>quotes</Name>
        <Prefix>N</Prefix>
        <Marker>Ned</Marker>
        <MaxKeys>40</MaxKeys>
        <IsTruncated>false</IsTruncated>
        <Contents>
          <Key>Nelson</Key>
          <LastModified>2006-01-01T12:00:00.000Z</LastModified>
          <ETag>&quot;828ef3fdfa96f00ad9f27c383fc9ac7f&quot;</ETag>
          <Size>5</Size>
          <StorageClass>STANDARD</StorageClass>
          <Owner>
            <ID>bcaf161ca5fb16fd081034f</ID>
            <DisplayName>webfile</DisplayName>
          </Owner>
        </Contents>
        <Contents>
          <Key>Neo</Key>
          <LastModified>2006-01-01T12:00:00.000Z</LastModified>
          <ETag>&quot;828ef3fdfa96f00ad9f27c383fc9ac7f&quot;</ETag>
          <Size>4</Size>
          <StorageClass>STANDARD</StorageClass>
          <Owner>
            <ID>bcaf1ffd86a5fb16fd081034f</ID>
            <DisplayName>webfile</DisplayName>
          </Owner>
        </Contents>
      </ListBucketResult>

      val result = ListObjects.fromXml(xml)
      result.isSuccess should be (true)
      val (info, items)= result.get
      info.bucket should be (BucketName("quotes"))
      info.prefix should be ("N")
      info.marker should be ("Ned")
      info.maxKeys should be (40)
      info.isTruncated should be (false)

      items should have length 2
      val item1 = items(0)
      item1.key should be (Key("Nelson"))
      item1.etag should be (ETag("\"828ef3fdfa96f00ad9f27c383fc9ac7f\""))
      item1.size should be (ByteSize(5))
      item1.storageClass should be (StorageClasses.Standard)
    }

  }
}