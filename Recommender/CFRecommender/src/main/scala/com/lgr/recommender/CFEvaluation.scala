package com.lgr.recommender

import breeze.numerics.sqrt
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
 * @author Li Gengrun
 * @date 2023/3/27 16:14
 */
object CFEvaluation {
  val MONGODB_RATING_COLLECTION = "Rating" //评分表
  val REC_RESULT = "RecommendationResults" //每个用户的推荐列表

  def main(args: Array[String]): Unit = {
    val sparkConfig = new SparkConf().setAppName("CFEvaluation").setMaster("local[*]")
    val spark = SparkSession.builder().config(sparkConfig).getOrCreate()
    // 在对 DataFrame 和 Dataset 进行操作许多操作都需要这个包进行支持
    import spark.implicits._
    //隐式参数，因为每次都要传
    implicit val mongoConfig = MongoConfig("mongodb://localhost:27017/recommender", "recommender")

    /**
     * 加评分数据
     */
    //评分的RDD
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql.DefaultSource")
      .load()
      .as[Ratings] //转为dataset
      .rdd //转为RDD格式
      .map(rating => Rating(rating.userId, rating.movieId, rating.score)) //去除timestamp
      .cache() //持久化到内存

    /**
     * 切分训练集和测试集
     */
    val splits = ratingRDD.randomSplit(Array(0.8, 0.2)) //80%的训练集，20%的测试集
    val trainingData = splits(0)
    val testData = splits(1)

    /**
     * 模型参数选择
     *
     * 目的是获得最优参数，rank lambda
     */
    AdjustParams(trainingData, testData)

    spark.close()
  }

  /**
   * 获得最优参数
   * @param trainingData 训练集
   * @param testData 测试集
   */
  def AdjustParams(trainingData: RDD[Rating], testData: RDD[Rating]): Unit = {
    val result = for (rank <- Array(10,100,1000,10000); lambda <- Array(0.01,0.1,1))
      yield {
        val model = ALS.train(trainingData, rank, 5, lambda) //训练模型
        val rmse = getRMSE(model, testData) //计算RMSE
        (rank, lambda, rmse) //保存这三个参数
      }
    println("最优参数："+result.minBy(_._3)) //根据rmse作比较，选取rmse最小的一组数据
    result.foreach { case (rank, lambda, rmse) =>
      println(s"Rank: $rank, Lambda: $lambda, RMSE: $rmse")
    }
  }

  /**
   * 计算 RMSE
   * @param model 训练好的模型
   * @param data  测试集
   * @return
   */
  def getRMSE(model: MatrixFactorizationModel, data: RDD[Rating]): Double = {
    //计算预测评分
    val userMovies = data.map(item => (item.user,item.product))
    val predictRating = model.predict(userMovies)
    //以(userId, movieID)为外键做内连接，从而可以实现预测值和观测值相减
    val observed = data.map(item => ((item.user,item.product),item.rating))
    val predict = predictRating.map(item => ((item.user,item.product),item.rating))
    // 计算 RMSE
    sqrt(
      //内连接得到的数据格式：(userId, movieId), (actual, pre)
      observed.join(predict).map{case ((userID,movieId),(actual,pre))=>
        // 真实值和预测值之间的差
        val err = actual - pre
        //平方
        err * err
      }.mean()
    )
  }
}
