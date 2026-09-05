package skygit.web

import scala.collection.mutable
import java.util.UUID

enum PipelineStatus:
    case Pending, Running, Completed, Failed, Canceled

case class PipelineJob(
    id: String,
    var status: PipelineStatus,
    logs: mutable.ArrayBuffer[String],
)

class PipelineService:
    private val jobs = mutable.Map.empty[String, PipelineJob]

    def startPipeline(): String =
        val id = UUID.randomUUID().toString

        val job = PipelineJob(
            id = id,
            status = PipelineStatus.Pending,
            logs = mutable.ArrayBuffer.empty[String]
        )

        jobs += (id -> job)

        id

    def getPipeline(id: String): Option[PipelineJob] =
        jobs.get(id)

    def getPipelineLogs(id: String): String =
        jobs.get(id) match
            case Some(job) => job.logs.mkString("\n")
            case None      => s"No logs found for pipeline $id"

    def appendLog(id: String, message: String): Unit =
        jobs.get(id).foreach { job =>
            job.logs += message
        }

    def setStatus(id: String, status: PipelineStatus): Unit =
        jobs.get(id).foreach { job =>
            job.status = status
        }

    def cancelPipeline(id: String): String =
        jobs.get(id) match
            case Some(job) =>
                job.status = PipelineStatus.Canceled
                s"Pipeline $id canceled"

            case None =>
                s"No pipeline found with ID $id"

// TODO: Implement proper pipeline management
// - Add streaming of pipeline logs to subscribers
// - manage the lifecycle of pipeline jobs
// - Add database persistence for pipeline jobs
// - Clean up after x days in db
// - Actually send pipeline to docker container

// Pipeline yaml format
// image: x
// steps:
//   - name: step1
//     run: echo "Hello, World!"
//     run: echo "Another step"

// Out of scope: 
// - Handling  pipeline dependencies
// - Integration with external CI/CD systems