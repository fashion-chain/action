package org.fok.action.test;

import org.apache.commons.lang3.StringUtils;
import org.fok.action.model.Test.ActionTestCommand;
import org.fok.action.model.Test.ActionTestModule;
import org.fok.action.model.Test.ReqStartBlockChain;
import org.fok.action.model.Test.RespStartBlockChain;
import org.fok.core.FokBlockChain;
import org.fok.core.cryptoapi.ICryptoHandler;

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
public class StartBlockChainImpl extends SessionModules<ReqStartBlockChain> {
	@ActorRequire(name = "fok_block_chain_core", scope = "global")
	FokBlockChain fokBlockChain;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@Override
	public String[] getCmds() {
		return new String[] { ActionTestCommand.STR.name() };
	}

	@Override
	public String getModule() {
		return ActionTestModule.ATI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqStartBlockChain pb, final CompleteHandler handler) {
		RespStartBlockChain.Builder oRespStartBlockChain = RespStartBlockChain.newBuilder();
		try {
			if (fokBlockChain.getNodeAccountAddress() != null) {
				oRespStartBlockChain.setRetCode(-1);
				oRespStartBlockChain.setRetMsg(
						"已设置节点账户为：" + crypto.bytesToHexStr(fokBlockChain.getNodeAccountAddress()) + ", 不允许重复设置");
			} else {
				if (StringUtils.isNotBlank(pb.getAddress())) {
					fokBlockChain.setNodeAccountAddress(crypto.hexStrToBytes(pb.getAddress()));
				}
				oRespStartBlockChain.setRetCode(1);
			}
			fokBlockChain.startBlockChain("forTest", "", "");
		} catch (Exception e) {
			log.error("", e);
			oRespStartBlockChain.setRetCode(-1);
			oRespStartBlockChain.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespStartBlockChain.build()));
	}
}
