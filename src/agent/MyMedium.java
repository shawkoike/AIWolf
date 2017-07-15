package agent;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

// 霊媒師役エージェントクラス
public class MyMedium extends MyVillager {
	// 何日目にカミングアウトするか
	int comingoutDay;
	// カミングアウト済みか
	boolean isCameOut;
	// 報告待ち霊媒結果の待ち行列
	Deque<Judge> identQueue = new LinkedList<>();
	// 霊媒結果をエージェントと種族のペアで格納
	Map<Agent, Species> myIdentMap = new HashMap<>();

	// 初期化
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		// 1~3日目のどこかでカミングアウト
		comingoutDay = (int)(Math.random()*3 + 1);
		isCameOut = false;
		identQueue.clear();
		myIdentMap.clear();
	}

	// 1日の始まりメソッド
	public void dayStart() {
		super.dayStart();
		// 霊媒結果を待ち行列に入れる
		Judge ident = currentGameInfo.getMediumResult();
		if(ident != null) {
			identQueue.offer(ident);
			myIdentMap.put(ident.getTarget(), ident.getResult());
		}
	}

	// 投票先選択
	protected void chooseVoteCandidate() {
		wereWolves.clear();
		// 霊媒結果をカミングアウトしている他のエージェントは人狼候補(自分が霊媒師なので嘘の確率大)
		for (Agent agent : aliveOthers) {
			if(comingoutMap.get(agent) == Role.MEDIUM) {
				wereWolves.add(agent);
			}
		}
		// 自分や殺されたエージェントを人狼と判定、あるいは自分と異なる判定の占い師は人狼候補
		for(Judge j : divinationList) {
			Agent agent = j.getAgent();
			Agent target = j.getTarget();
			if(j.getResult() == Species.WEREWOLF && (target == me || isKilled(target)) ||
					(myIdentMap.containsKey(target) && j.getResult() != myIdentMap.get(target))) {
				if(isAlive(agent) && !wereWolves.contains(agent)) {
					wereWolves.add(agent);
				}
			}
		}
		// 候補がいない場合はランダム
		if(wereWolves.isEmpty()) {
			if(!aliveOthers.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
			}
		} else {
			if(!wereWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
				// 以前の投票先から変わる場合、新たに推測発言と占い要請をする
				if(canTalk) {
					talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
					talkQueue.offer(new Content(new RequestContentBuilder(null, new Content(new DivinationContentBuilder(voteCandidate)))));
				}
			}
		}
	}
}
