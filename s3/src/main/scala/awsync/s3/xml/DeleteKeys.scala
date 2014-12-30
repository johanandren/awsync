package awsync.s3.xml

import awsync.s3.Key

import scala.xml.Elem

private[s3] object DeleteKeys {

  def toXml(keys: Seq[Key]): Elem =
    <Delete>
      {keys.map(key =>
        <Object>
          <Key>{key.name}</Key>
        </Object>
      )}
    </Delete>

}
