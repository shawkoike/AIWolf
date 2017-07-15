package agent;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

// 騎士役エージェントクラス
public class MyBodyGuard extends MyVillager {

	// 護衛したエージェント
	Agent guardedAgent;

	// 初期化
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		guardedAgent = null;
	}

	// 騎士行為
	public Agent guard() {
		Agent guardCandidate = null;
		// 前日の護衛が成功しているならば同じエージェントを護衛 <= 狐がいない場合のみ確定事項
		if(guardedAgent != null && isAlive(guardedAgent) && currentGameInfo.getLastDeadAgentList().isEmpty()) {
			guardCandidate = guardedAgent;
		}
		// 新しい護衛先の選定
		else {
			// 占い師をカミングアウトしていて、かつ人狼候補になっていないエージェントを探す
			List<Agent> candidates = new ArrayList<>();
			for(Agent agent:aliveOthers) {
				if(comingoutMap.get(agent) == Role.SEER && !wereWolves.contains(agent)) {
					candidates.add(agent);
				}
			}
			// 見つからなければ霊媒師をカミングアウトしていて、かつ人狼候補になっていないエージェントを探す
			if(candidates.isEmpty()) {
				for(Agent agent: aliveOthers) {
					if(comingoutMap.get(agent) == Role.MEDIUM && !wereWolves.contains(agent)) {
						candidates.add(agent);
					}
				}
			}
			// それでも見つからなければ自分と人狼候補以外から護衛
			if(candidates.isEmpty()) {
				for(Agent agent : aliveOthers) {
					if(!wereWolves.contains(agent)) {
						candidates.add(agent);
					}
				}
			}
			// それでもいなければ自分以外から護衛
			if(candidates.isEmpty()) {
				candidates.addAll(aliveOthers);
			}
			// 護衛候補からランダムに護衛
			guardCandidate = randomSelect(candidates);
		}
		guardedAgent = guardCandidate;
		return guardCandidate;
	}
}
