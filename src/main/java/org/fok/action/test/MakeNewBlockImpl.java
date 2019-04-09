package org.fok.action.test;

import org.fok.action.model.Test.ActionTestCommand;
import org.fok.action.model.Test.ActionTestModule;
import org.fok.action.model.Test.ReqMakeNewBlock;
import org.fok.action.model.Test.RespMakeNewBlock;
import org.fok.core.FokBlock;
import org.fok.core.FokBlockChain;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Block.BlockInfo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class MakeNewBlockImpl extends SessionModules<ReqMakeNewBlock> {
	@ActorRequire(name = "fok_block_core", scope = "global")
	FokBlock fokBlock;
	@ActorRequire(name = "fok_block_chain_core", scope = "global")
	FokBlockChain fokBlockChain;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@Override
	public String[] getCmds() {
		return new String[] { ActionTestCommand.NEB.name() };
	}

	@Override
	public String getModule() {
		return ActionTestModule.ATI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqMakeNewBlock pb, final CompleteHandler handler) {
		RespMakeNewBlock.Builder oRespMakeNewBlock = RespMakeNewBlock.newBuilder();
		try {
			if (fokBlockChain.getBlockByHeight(0) == null) {
				oRespMakeNewBlock.setRetCode(-1);
				oRespMakeNewBlock.setRetMsg("不存在创世块。请先创建创世块");
			} else {
				BlockInfo bi = fokBlock.createBlock(pb.getTxCount(), pb.getMinConfirmed(), null, null);
				oRespMakeNewBlock.setBlockHash(crypto.bytesToHexStr(bi.getHeader().getHash().toByteArray()));
				oRespMakeNewBlock.setHeight(String.valueOf(bi.getHeader().getHeight()));
				oRespMakeNewBlock.setParentHash(crypto.bytesToHexStr(bi.getHeader().getParentHash().toByteArray()));
				oRespMakeNewBlock.setStateRoot(crypto.bytesToHexStr(bi.getHeader().getStateRoot().toByteArray()));
				oRespMakeNewBlock.setTxRoot(crypto.bytesToHexStr(bi.getHeader().getTransactionRoot().toByteArray()));
				oRespMakeNewBlock.setReceiptRoot(crypto.bytesToHexStr(bi.getHeader().getReceiptRoot().toByteArray()));
				oRespMakeNewBlock.setRetCode(1);
			}
		} catch (Exception e) {
			log.error("", e);
			oRespMakeNewBlock.setRetMsg(e.getMessage());
			oRespMakeNewBlock.setRetCode(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespMakeNewBlock.build()));
	}
}
