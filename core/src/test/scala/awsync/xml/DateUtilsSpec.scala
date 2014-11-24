package awsync.xml

import awsync.AbstractSpec

class DateUtilsSpec extends AbstractSpec {

  describe("the xml date utils") {

    it("parses an aws timestamp string") {
      val result = DateUtils.parseTimestamp("2013-09-30T11:17:16.000Z")

      result.isSuccess should be (true)
    }

  }
}
