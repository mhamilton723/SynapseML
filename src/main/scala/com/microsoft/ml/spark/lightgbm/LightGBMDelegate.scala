// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.ml.spark.lightgbm

import com.microsoft.ml.lightgbm.SWIGTYPE_p_void
import org.slf4j.Logger

trait LightGBMDelegate extends Serializable {
  def beforeTrainIteration(partitionId: Int, curIters: Int, log: Logger, trainParams: TrainParams,
                           boosterPtr: Option[SWIGTYPE_p_void], hasValid: Boolean): Unit = {
    // override this function and write code
  }

  def afterTrainIteration(partitionId: Int, curIters: Int, log: Logger, trainParams: TrainParams,
                          boosterPtr: Option[SWIGTYPE_p_void], hasValid: Boolean, isFinished: Boolean,
                          trainEvalResults: Option[Map[String, Double]],
                          validEvalResults: Option[Map[String, Double]]): Unit = {
    // override this function and write code
  }

  def getLearningRate(partitionId: Int, curIters: Int, log: Logger, trainParams: TrainParams,
                      previousLearningRate: Double): Double = {
    // override this function and write code
    previousLearningRate
  }
}
