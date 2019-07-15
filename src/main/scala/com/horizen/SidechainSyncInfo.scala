package com.horizen

import com.google.common.primitives.Ints
import com.horizen.block.SidechainBlock
import com.horizen.utils.BytesUtils
import scorex.core.NodeViewModifier
import scorex.core.consensus.History.ModifierIds
import scorex.util.ModifierId
import scorex.core.consensus.SyncInfo
import scorex.core.serialization.Serializer
import scorex.util.{idToBytes, bytesToId}

import scala.util.Try

case class SidechainSyncInfo(knownBlockIds: Seq[ModifierId]) extends SyncInfo {
  override type M = SidechainSyncInfo

  override def serializer: Serializer[SidechainSyncInfo] = SidechainSyncInfoSerializer

  // get most recent block
  override def startingPoints: ModifierIds = {
    knownBlockIds.lastOption match {
      case Some(id) => Seq(SidechainBlock.ModifierTypeId -> id)
      case None => Seq()
    }
  }
}


object SidechainSyncInfoSerializer extends Serializer[SidechainSyncInfo] {
  override def toBytes(obj: SidechainSyncInfo): Array[Byte] = {
    Ints.toByteArray(NodeViewModifier.ModifierIdSize * obj.knownBlockIds.size) ++
    obj.knownBlockIds.foldLeft(Array[Byte]())((a, b) => a ++ idToBytes(b))
  }

  override def parseBytes(bytes: Array[Byte]): Try[SidechainSyncInfo] = Try {
    val length = BytesUtils.getInt(bytes, 0)
    if(bytes.length != length + 4)
      throw new IllegalArgumentException("Input data corrupted.")
    var currentOffset: Int = 4

    var modifierIds: Seq[ModifierId] = Seq()

    while(currentOffset < bytes.length) {
      modifierIds = modifierIds :+ bytesToId(BytesUtils.reverseBytes(bytes.slice(currentOffset, currentOffset + NodeViewModifier.ModifierIdSize)))
      currentOffset += NodeViewModifier.ModifierIdSize
    }

    SidechainSyncInfo(modifierIds)
  }

}