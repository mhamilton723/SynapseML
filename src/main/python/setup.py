# Copyright (C) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See LICENSE in project root for information.

import os
from setuptools import setup, find_packages

setup(
    name="dciborowMMLSpark",
    version="0.0.23",
    description="Microsoft ML for Spark",
    long_description="Microsoft ML for Apache Spark contains Microsoft's open source " +
                     "contributions to the Apache Spark ecosystem",
    license="MIT",
    # Project's main homepage.
    url="https://github.com/dciborow/mmlspark",
    # Author details
    author="Microsoft",
    author_email="mmlspark-support@microsoft.com",

    classifiers=[
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3"
    ],
    
     packages=['mmlspark',
               'mmlspark.recommendation',
               'mmlspark.jars'],
    include_package_data=True,
    package_dir={
            'mmlspark.jars': 'deps/jars',
    },
    package_data={
            'mmlspark.jars': ['*.jar']},

    install_requires=[
        "pyspark",
        "numpy"
    ],

    zip_safe=True
)
