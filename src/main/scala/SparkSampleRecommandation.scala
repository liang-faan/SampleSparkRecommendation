import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd._
import org.apache.spark.sql.SparkSession
//import scalacl._

object SparkSampleRecommandation {
  val dataPath = "./Data/"

  case class Record(movieId: Int, movieName: String)

  /**
   *
   * @Ratings Data File Structure
   * UserID::MovieID::Rating::Timestamp
   *
   *
   * @Tags Data File Structure
   * UserID::MovieID::Tag::Timestamp
   *
   *
   * Movies Data File Structure
   * MovieID::Title::Genres
   */

  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder()
      .appName("Spark Recommendation")
      .master("local[*]")
      .getOrCreate();

    //    val moviesDf = sparkSession.read.text(dataPath + "magazines.dat")
    //      .toDF("value").withColumn("value", split(col("value"), "::")).select(
    //      col("value").getItem(0).as("movieId"),
    //      col("value").getItem(1).as("movieName")
    //    )

    val magazines = sparkSession.sparkContext.textFile(dataPath + "magazines.dat").map {
      line =>
        val fields = line.split("::");
        (fields(0).toInt, fields(1))
    }.collect().toMap


    val ratings = sparkSession.sparkContext.textFile(dataPath + "magazine_ratings.dat").map {
      line =>
        val fields = line.split("::");
        (fields(3).toInt % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
    }

    val numRatings = ratings.count
    val numUsers = ratings.map(_._2.user).distinct.count
    val numMovies = ratings.map(_._2.product).distinct.count

    println("Got " + numRatings + " ratings from "
      + numUsers + " users on " + numMovies + " movies.")

    val training = ratings.filter(x => x._1 < 6)
      .values
      .cache()
    val validation = ratings.filter(x => x._1 >= 6 && x._1 < 8)
      .values
      .cache()
    val test = ratings.filter(x => x._1 >= 8).values.cache()

    val numTraining = training.count()
    val numValidation = validation.count()
    val numTest = test.count()

    println("Training: " + numTraining + ", validation: " + numValidation + ", test: " + numTest)

    val ranks = List(8, 12)
    val lambdas = List(0.1, 10.0)
    val numIters = List(10, 10)
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
      val model = ALS.train(training, rank, numIter, lambda)
      val validationRmse = computeRmse(model, validation, numValidation)
      println("RMSE (validation) = " + validationRmse + " for the model trained with rank = "
        + rank + ", lambda = " + lambda + ", and numIter = " + numIter + ".")
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }

    bestModel.get.save(sparkSession.sparkContext, "./RecommendationModel/")

    // evaluate the best model on the test set
    val testRmse = computeRmse(bestModel.get, test, numTest)

    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
      + ", and numIter = " + bestNumIter + ", and its RMSE on the test set is " + testRmse + ".")

    // create a naive baseline and compare it with the best model
    val meanRating = training.union(validation).map(_.rating).mean
    val baselineRmse =
      math.sqrt(test.map(x => (meanRating - x.rating) * (meanRating - x.rating)).mean)
    val improvement = (baselineRmse - testRmse) / baselineRmse * 100
    println("The best model improves the baseline by " + "%1.2f".format(improvement) + "%.")


    val candidates = sparkSession.sparkContext.parallelize(magazines.keys.toSeq)

    candidates.map((100, _)).collect().foreach((i) => print("for each"+i))


    val recommendations = bestModel.get
      .predict(candidates.map((100, _)))
      .collect()
      .sortBy(- _.rating)
      .take(20)


    var i = 1
    println("Magazines recommended for you:")
    recommendations.foreach { r =>
      println("%2d".format(i) + ": " + magazines(r.product))
      i += 1
    }

  }

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating))).values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }
}
