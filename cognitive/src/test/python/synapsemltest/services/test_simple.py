# Copyright (C) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See LICENSE in project root for information.

# Prepare training and test data.
import unittest

from synapse.ml.io.http import *
from synapse.ml.core.init_spark import *
from pyspark.sql.functions import struct
from pyspark.sql.types import *

from pyspark.sql import SQLContext
from synapse.ml.core.init_spark import *

spark = init_spark()
sc = SQLContext(spark.sparkContext)


class SimpleHTTPTransformerSmokeTest(unittest.TestCase):
    def test_simple(self):
        df = spark.createDataFrame([("foo",) for x in range(20)], ["data"]).withColumn(
            "inputs",
            struct("data"),
        )

        response_schema = (
            StructType().add("status", StringType()).add("message", StringType())
        )

        client = (
            SimpleHTTPTransformer()
            .setInputCol("inputs")
            .setInputParser(JSONInputParser())
            .setOutputParser(JSONOutputParser().setDataType(response_schema))
            .setOutputCol("results")
            .setUrl("https://dog.ceo/api/breeds/image/random")
        )

        responses = client.transform(df)
        responses.select("results").show(truncate=False)


if __name__ == "__main__":
    result = unittest.main()
