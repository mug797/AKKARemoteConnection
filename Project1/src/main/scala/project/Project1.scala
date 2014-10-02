package project

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.Scheduler
import akka.routing.RoundRobinRouter
import scala.util.control.Breaks._
import scala.concurrent.duration._
import akka.actor.ActorRef

case class RequestWork (ip: String)
case object StartMining 
case class Message(msg: String)

class Worker extends Actor {
  println("Spawned worker")
  def receive = {

    case RequestWork(ip) =>
      println("Connecting to Master for work at "+ip)
      val remote = context.actorSelection("akka.tcp://MasterBitcoinSystem@"+ip+":6000/user/Master")
      remote ! Message("GIVEWORK")
    case Message(msg) =>
    //println(msg)
     val array = msg.split('|')
    if(array(0) == ("TAKEWORK")){
      //println("Doing work now ..."+array(2))
  
     val k = array(1).toInt
     val gatorId = array(2)
     val start = array(3).toInt
     val batchSize = array(4).toInt
  
      mineForCoins(k ,gatorId, start, batchSize)
    }
  }

  def mineForCoins(k:Int, gatorId: String, start: Int, batchSize: Int) {
      
    var MAX_ZERO = 0
    var MAX_HASH = ""
    var i = start
    var end = start + batchSize
    for(i <- start to end){
      val input = gatorId+i
      val md = java.security.MessageDigest.getInstance("SHA-256")
      val hash = md.digest(input.getBytes("UTF-8")).map("%02x".format(_)).mkString
      
      val array = hash.split("")
      val hashLength = hash.length
        
      var countZero = 0
        
      breakable {
        for( j <- 0 to hashLength){
         if(array(j) == "0"){
           countZero = countZero + 1
         } else {
            break
        }
      }
    }
        
    if(countZero >= k){
      sender ! Message("FOUND|"+input+"\t"+hash)
    }
     
    if(countZero >= MAX_ZERO){
      MAX_HASH = hash
      MAX_ZERO = countZero
    }
  }
    
  if(MAX_ZERO >= k){
    sender ! Message("MAXHASH|"+MAX_ZERO+"|"+MAX_HASH)
  }
    sender ! Message("GIVEWORK")
  }
}

class Listener extends Actor {
  import context.dispatcher
  
    def receive = {
      case Message(msg) =>
        var arr = msg.toString.split('|')
        if(arr(0) == "START"){
        	println("STARTED LISTNER!!!!!!!!!!!!")
        	context.system.scheduler.schedule(300.seconds,30.seconds, sender, Message("SHUTDOWN"))
        }
    }
  }


class Master(k: Int, gatorId: String, noOfWorker: Int, batchSize: Int, worker:ActorRef) extends Actor {
  var MAX_HASH = ""
  var MAX_ZERO = 0
  var bitcoinCount = 0
  println("Spawned Master")
  var start = 0

  
  def receive = {
    case StartMining =>
       val listener = context.actorOf(Props[Listener], name = "listener")
       listener ! Message("START")
     println("Master is Mining now ...")
      var i = noOfWorker
//      while(i > 0){
      //println("spawn worker "+start)
//       worker ! Message("TAKEWORK|"+k+"|"+gatorId+"|"+start+"|"+batchSize)
//       start= start + batchSize
//      i = i-1
//      }

   case Message(msg) =>
        var arr = msg.toString.split('|')
        if(arr(0) == "GIVEWORK"){
          
            //println("Giving work")
            sender ! Message("TAKEWORK|"+k+"|"+gatorId+"|"+start+"|"+batchSize)
            start= start + batchSize
        } else if(arr(0) == "FOUND"){
          bitcoinCount = bitcoinCount +1
          println(arr(1))
        } else if (arr(0) == "MAXHASH"){
          //println("Maximun "+arr(1)+" leading zeros in "+ arr(2))
          val zeros = arr(1).toInt
          val hash = arr(2)
          if(zeros >= MAX_ZERO){
            MAX_HASH = hash
            MAX_ZERO = zeros
          }
        } else if (arr(0) == "SHUTDOWN"){
          println("---------------------------------------------")
          println("Leading Zeros Required\t\t:\t"+k+
            		"\nNo of Local Workers Used\t:\t"+noOfWorker+
            		"\nBatch Size Used\t\t\t:\t"+batchSize+
            		"\nBitcoin with Max No of 0s\t:\t"+MAX_ZERO+
            		"\nMax 0s Hash Value\t\t:\t"+MAX_HASH+
            		"\nTotal Bitcoins Found\t\t:\t"+bitcoinCount+
            		"\nTotal Hash Calculated\t\t:\t"+start)
          println("---------------------------------------------")
          context.system.shutdown();
        } 
    case _ =>
      println("ERROR : Unexpected Message ")
  }
}


object Project1 extends App {
  val arg = args(0)

  println("Checking for running mode ...")

  if(arg.contains('.')){
    println("Remote Mode Running...")
    val ip = arg
    // spawn worker
    //connect to ip provided
    implicit val system = ActorSystem("SlaveBitcoinSystem")
    val worker = system.actorOf(Props[Worker], name = "worker")  
    worker ! RequestWork(ip)

  }
  else{
    println("Normal mode Running...")

    val gatorId = "pratik.s"
    val batchSize = 50000
    val k  = args(0).toInt
    val noOfWorker = 12

    implicit val system = ActorSystem("MasterBitcoinSystem")
    // spawn master
    //pass k to master
    val worker = system.actorOf(Props[Worker].withRouter(RoundRobinRouter(noOfWorker)),
   name = "worker")
    
    
    val master = system.actorOf(Props(new Master(k, gatorId, noOfWorker, batchSize, worker)), name = "Master")
    master ! StartMining
     
  }                                                      
}


