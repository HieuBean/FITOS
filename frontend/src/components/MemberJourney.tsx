export default function MemberJourney() {
  const steps = [
    { num: '01', title: 'Sign up', desc: 'Create your account and choose a membership plan that fits your goals.' },
    { num: '02', title: 'Book a class', desc: 'Browse the schedule and reserve your spot in any class.' },
    { num: '03', title: 'Train with coaches', desc: 'Expert trainers guide every session for results and safety.' },
    { num: '04', title: 'Track progress', desc: 'Log workouts, monitor metrics, and celebrate milestones.' },
  ];
  return (
    <section className="bg-[#171A16] py-20">
      <div className="max-w-[1280px] mx-auto px-10">
        <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#D7FF3F] mb-3">HOW IT WORKS</p>
        <h2 className="text-[38px] font-bold text-white mb-4">Your fitness journey</h2>
        <p className="text-base text-[#A8ADA2] max-w-xl mb-14">From first sign-up to consistent training — FITOS supports every step.</p>
        <div className="flex flex-wrap gap-0">
          {steps.map((step, i) => (
            <div key={step.num} className="flex items-start gap-6 flex-1 min-w-[200px]">
              <div className="flex-1">
                <div className="inline-flex items-center gap-2 bg-[#D7FF3F] text-[#111310] rounded-full px-3 py-1 text-xs font-bold mb-4">{step.num}</div>
                <h3 className="text-lg font-bold text-white mb-2">{step.title}</h3>
                <p className="text-sm text-[#A8ADA2]">{step.desc}</p>
              </div>
              {i < steps.length - 1 && (
                <div className="hidden lg:flex items-center pt-10 flex-shrink-0">
                  <span className="text-[#D7FF3F] text-2xl">→</span>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
