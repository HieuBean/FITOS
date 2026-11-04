import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  getExercises,
  getWorkoutPlans,
  getProgressSummary,
  getWorkoutSessions,
  sendAiChat,
  requestAiRecommendation,
  acceptAiRecommendation,
  dismissAiRecommendation
} from '../src/lib/api.ts';

describe('Layer 3 Frontend Unit Test Suite - API Lib & Mock Fallbacks', () => {

  describe('getExercises()', () => {
    it('should return list of default exercises with required fields', async () => {
      const exercises = await getExercises();
      assert.ok(Array.isArray(exercises), 'exercises must be an array');
      assert.ok(exercises.length >= 10, 'must have at least 10 exercises');
      
      const first = exercises[0];
      assert.strictEqual(typeof first.id, 'number');
      assert.strictEqual(typeof first.name, 'string');
      assert.strictEqual(typeof first.muscleGroup, 'string');
      assert.strictEqual(typeof first.difficulty, 'string');
    });

    it('should contain key compound exercises like Bench Press and Barbell Squat', async () => {
      const exercises = await getExercises();
      const names = exercises.map(e => e.name);
      assert.ok(names.includes('Bench Press'), 'should include Bench Press');
      assert.ok(names.includes('Barbell Squat'), 'should include Barbell Squat');
      assert.ok(names.includes('Pull Up'), 'should include Pull Up');
    });
  });

  describe('getWorkoutPlans()', () => {
    it('should return member workout plan with exercises', async () => {
      const plans = await getWorkoutPlans(1);
      assert.ok(Array.isArray(plans), 'plans must be an array');
      assert.ok(plans.length > 0, 'should have default workout plan');

      const plan = plans[0];
      assert.strictEqual(plan.customerId, 1);
      assert.ok(plan.name, 'plan must have a title/name');
      assert.ok(Array.isArray(plan.exercises), 'plan must have exercises array');
      assert.ok(plan.exercises!.length > 0, 'plan should have exercises');

      const ex = plan.exercises![0];
      assert.ok(ex.exerciseName, 'exercise item must have name');
      assert.ok(ex.sets > 0, 'target sets must be positive');
      assert.ok(ex.reps > 0, 'target reps must be positive');
    });
  });

  describe('getProgressSummary()', () => {
    it('should return member progress metrics and personal records', async () => {
      const summary = await getProgressSummary(1);
      assert.strictEqual(summary.customerId, 1);
      assert.strictEqual(typeof summary.streak, 'number');
      assert.strictEqual(typeof summary.totalSessionsCompleted, 'number');
      assert.ok(Array.isArray(summary.personalRecords), 'personalRecords must be an array');
      assert.ok(summary.personalRecords.length > 0, 'should have PR entries');

      const pr = summary.personalRecords[0];
      assert.ok(pr.exerciseName);
      assert.ok(pr.maxWeightKg > 0);
    });
  });

  describe('getWorkoutSessions()', () => {
    it('should return workout session history with durations and calories', async () => {
      const sessions = await getWorkoutSessions(1);
      assert.ok(Array.isArray(sessions), 'sessions must be an array');
      assert.ok(sessions.length > 0, 'should have past workout sessions');

      const s = sessions[0];
      assert.strictEqual(s.customerId, 1);
      assert.ok(s.startedAt);
      assert.ok(s.status);
      assert.ok((s.caloriesBurned ?? 0) > 0);
    });
  });

  describe('AI Coach & Recommendation Functions', () => {
    it('sendAiChat should return a valid conversational reply with suggestions', async () => {
      const res = await sendAiChat('Hôm nay tôi muốn tập ngực');
      assert.ok(res.reply, 'should return reply text');
      assert.ok(Array.isArray(res.suggestions), 'should return suggestions array');
      assert.ok(res.suggestions.length > 0, 'suggestions should not be empty');
    });

    it('requestAiRecommendation should return structured recommendation payload', async () => {
      const rec = await requestAiRecommendation(1, 'MUSCLE_GAIN', 'CHEST');
      assert.ok(rec.title, 'recommendation must have title');
      assert.ok(rec.rawJson, 'recommendation must contain json payload');
      
      const parsed = JSON.parse(rec.rawJson);
      assert.ok(Array.isArray(parsed.suggested_changes), 'must have suggested_changes');
      assert.ok(parsed.suggested_changes.length > 0);
    });
  });
});
