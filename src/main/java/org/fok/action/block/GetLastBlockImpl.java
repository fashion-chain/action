package org.fok.action.block;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.fok.actionmodel.Action.ActionCommand;
import org.fok.actionmodel.Action.ActionModule;
import org.fok.actionmodel.Action.BlockHashMessage;
import org.fok.actionmodel.Action.BlockHeightMessage;
import org.fok.actionmodel.Action.BlockMessage;
import org.fok.actionmodel.BlockImpl.BlockBodyImpl;
import org.fok.actionmodel.BlockImpl.BlockHeaderImpl;
import org.fok.actionmodel.BlockImpl.BlockInfoImpl;
import org.fok.actionmodel.BlockImpl.BlockMinerImpl;
import org.fok.actionmodel.TransactionImpl.TransactionBodyImpl;
import org.fok.actionmodel.TransactionImpl.TransactionInfoImpl;
import org.fok.actionmodel.TransactionImpl.TransactionInputImpl;
import org.fok.actionmodel.TransactionImpl.TransactionNodeImpl;
import org.fok.actionmodel.TransactionImpl.TransactionOutputImpl;
import org.fok.actionmodel.TransactionImpl.TransactionSignatureImpl;
import org.fok.core.FokBlockChain;
import org.fok.core.FokTransaction;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;

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
public class GetLastBlockImpl extends SessionModules<BlockHashMessage> {
	@ActorRequire(name = "fok_block_chain_core", scope = "global")
	FokBlockChain fokBlockChain;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "fok_transaction_core", scope = "global")
	FokTransaction fokTransaction;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCommand.GLB.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.API.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final BlockHashMessage pb, final CompleteHandler handler) {
		BlockMessage.Builder oBlockMessage = BlockMessage.newBuilder();

		try {
			BlockInfoImpl.Builder bii = BlockInfoImpl.newBuilder();
			BlockHeaderImpl.Builder bhi = bii.getHeaderBuilder();
			BlockMinerImpl.Builder bmi = bii.getMinerBuilder();
			BlockBodyImpl.Builder bbi = bii.getBodyBuilder();

			BlockInfo bi = fokBlockChain.getMaxConnectBlock();
			bhi.setHash(crypto.bytesToHexStr(bi.getHeader().getHash().toByteArray()));
			bhi.setParentHash(crypto.bytesToHexStr(bi.getHeader().getParentHash().toByteArray()));
			bhi.setHeight(bi.getHeader().getHeight());
			bhi.setTimestamp(bi.getHeader().getTimestamp());
			bhi.setExtraData(crypto.bytesToHexStr(bi.getHeader().getExtraData().toByteArray()));
			bhi.setReceiptRoot(crypto.bytesToHexStr(bi.getHeader().getReceiptRoot().toByteArray()));
			bhi.setTransactionRoot(crypto.bytesToHexStr(bi.getHeader().getTransactionRoot().toByteArray()));
			bhi.setStateRoot(crypto.bytesToHexStr(bi.getHeader().getStateRoot().toByteArray()));
			for (ByteString txHash : bi.getHeader().getTxHashsList()) {
				bhi.addTxHashs(crypto.bytesToHexStr(txHash.toByteArray()));
			}
			bii.setHeader(bhi);

			if (StringUtils.isNotBlank(pb.getContent()) && pb.getContent().equals("full")) {
				for (ByteString txHash : bi.getHeader().getTxHashsList()) {
					TransactionInfo oInfo = fokTransaction.getTransaction(txHash.toByteArray());
					TransactionInput oInput = oInfo.getBody().getInputs();
					List<TransactionOutput> oOutputs = oInfo.getBody().getOutputsList();

					TransactionInfoImpl.Builder tInfo = TransactionInfoImpl.newBuilder();
					TransactionBodyImpl.Builder tBody = tInfo.getBodyBuilder();
					TransactionInputImpl.Builder tInput = tBody.getInputsBuilder();
					TransactionNodeImpl.Builder tNode = tInfo.getNodeBuilder();

					tInfo.setHash(crypto.bytesToHexStr(oInfo.getHash().toByteArray()));
					tInfo.setResult(crypto.bytesToHexStr(oInfo.getResult().toByteArray()));
					tInfo.setStatus(oInfo.getStatus());

					tBody.setTimestamp(oInfo.getBody().getTimestamp());
					tBody.setType(oInfo.getBody().getType());
					
					TransactionSignatureImpl.Builder oTransactionSignatureImpl = TransactionSignatureImpl.newBuilder();
					oTransactionSignatureImpl
							.setSignature(crypto.bytesToHexStr(oInfo.getBody().getSignatures().getSignature().toByteArray()));
					tBody.setSignatures(oTransactionSignatureImpl);

					tNode.setAddress(crypto.bytesToHexStr(oInfo.getNode().getAddress().toByteArray()));
					tNode.setNode(oInfo.getNode().getNid());

					tInput.setAddress(crypto.bytesToHexStr(oInput.getAddress().toByteArray()));
					tInput.setNonce(oInput.getNonce());
					tInput.setAmount(BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).longValue());

					if (oInput.getSymbol() != null && !oInput.getSymbol().equals(ByteString.EMPTY)) {
						tInput.setSymbol(crypto.bytesToHexStr(oInput.getSymbol().toByteArray()));
					}
					if (oInput.getCryptoTokenCount() > 0) {
						for (ByteString cryptoToken : oInput.getCryptoTokenList()) {
							tInput.addCryptoToken(crypto.bytesToHexStr(cryptoToken.toByteArray()));
						}
					}
					tBody.setInputs(tInput);

					if (oOutputs != null && oOutputs.size() > 0) {
						for (int i = 0; i < oOutputs.size(); i++) {
							TransactionOutputImpl.Builder tOutput = TransactionOutputImpl.newBuilder();
							tOutput.setAddress(crypto.bytesToHexStr(oOutputs.get(i).getAddress().toByteArray()));
							tOutput.setAmount(BytesHelper.bytesToBigInteger(oOutputs.get(i).getAmount().toByteArray()).longValue());
							if (oOutputs.get(i).getCryptoTokenCount() > 0) {
								for (ByteString cryptoToken : oOutputs.get(i).getCryptoTokenList()) {
									tOutput.addCryptoToken(crypto.bytesToHexStr(cryptoToken.toByteArray()));
								}
							}
							tBody.addOutputs(tOutput);
						}
					}
					
					tInfo.setBody(tBody);
					tInfo.setNode(tNode);
					bbi.addTxs(tInfo);
				}
				
				bii.setBody(bbi);
			}
			
			bmi.setAddress(crypto.bytesToHexStr(bi.getMiner().getAddress().toByteArray()));
			bmi.setReward(BytesHelper.bytesToBigInteger(bi.getMiner().getReward().toByteArray()).longValue());
			bii.setMiner(bmi);

			oBlockMessage.addBlock(bii);
		} catch (Exception e) {
			log.error("", e);
			oBlockMessage.clear();
			oBlockMessage.setRetCode(-1);
			oBlockMessage.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oBlockMessage.build()));
	}
}
