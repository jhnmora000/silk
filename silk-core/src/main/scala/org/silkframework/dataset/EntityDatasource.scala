package org.silkframework.dataset

import org.silkframework.config.Task
import org.silkframework.entity.{Entity, EntitySchema, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

/**
  * A data source on [[org.silkframework.entity.Entity]] objects
  */
case class EntityDatasource(underlyingTask: Task[DatasetSpec[Dataset]], entities: Traversable[Entity], entitySchema: EntitySchema) extends DataSource with PeakDataSource {
  override def retrieve(requestSchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext): Traversable[Entity] = {
    if(requestSchema.typeUri != entitySchema.typeUri) {
      throw new ValidationException("Type URI '" + requestSchema.typeUri.toString + "' not available!")
    } else {
      val matchingPaths = entitySchema.typedPaths.zipWithIndex.filter { case (path, _) =>
          requestSchema.typedPaths.contains(path)
      }
      val matchingPathMap = matchingPaths.toMap
      if(matchingPaths.size != requestSchema.typedPaths.size) {
        val missingPath = requestSchema.typedPaths.find(tp => !matchingPathMap.contains(tp))
        throw new ValidationException("Some requested paths do not exist in data source, e.g. " + missingPath.get.serialize() + "!")
      } else {
        val matchingPathMap = matchingPaths.toMap
        val valuesIndexes = requestSchema.typedPaths.map ( tp => matchingPathMap(tp) )
        entities.map { entity =>
          Entity(
            entity.uri,
            valuesIndexes map { idx => entity.values(idx) },
            requestSchema
          )
        }
      }
    }
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext): Seq[Entity] = {
    throw new RuntimeException("Retrieve by URI is not supported!")
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {
    Seq(entitySchema.typeUri.uri -> 1.0)
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    entitySchema.typedPaths
  }
}
