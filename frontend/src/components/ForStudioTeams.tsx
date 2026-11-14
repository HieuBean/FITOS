export default function ForStudioTeams() {
  const features = [
    {
      title: 'CRM & lead management',
      desc: 'Capture leads, track conversion, and manage the entire member sales pipeline in one view.',
    },
    {
      title: 'Smart scheduling',
      desc: 'Build class timetables, assign trainers, manage capacity, and handle cancellations with ease.',
    },
    {
      title: 'Revenue insights',
      desc: 'Live dashboards for MRR, membership churn, class revenue, and PT session performance.',
    },
  ];
  const metrics = [
    { value: '3×', label: 'Lead-to-member conversion' },
    { value: '47%', label: 'Reduction in admin time' },
    { value: '£12k', label: 'Avg. monthly revenue tracked' },
    { value: '99.9%', label: 'Uptime SLA' },
  ];
  return (
    <section className="bg-[#F5F5F0] py-20">
      <div className="max-w-[1280px] mx-auto px-10">
        <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">FOR STUDIO TEAMS</p>
        <h2 className="text-[38px] font-bold text-[#111310] mb-4">Run your studio smarter</h2>
        <p className="text-base text-[#5E625A] max-w-xl mb-12">Built for gym owners and their teams — from front-desk staff to head coaches.</p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-14">
          {features.map((f) => (
            <div key={f.title} className="bg-white border border-[#DADCD3] rounded-xl p-6">
              <div className="w-10 h-10 bg-[#171A16] rounded-xl flex items-center justify-center mb-4">
                <span className="text-[#D7FF3F] font-bold text-base">★</span>
              </div>
              <h3 className="text-base font-bold text-[#111310] mb-2">{f.title}</h3>
              <p className="text-sm text-[#5E625A]">{f.desc}</p>
            </div>
          ))}
        </div>
        {/* Metrics bar */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 bg-white border border-[#DADCD3] rounded-2xl p-8">
          {metrics.map((m) => (
            <div key={m.label} className="text-center">
              <p className="text-3xl font-bold text-[#111310] mb-1">{m.value}</p>
              <p className="text-xs text-[#5E625A]">{m.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
