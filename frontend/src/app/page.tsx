import Header from '@/components/Header';
import Hero from '@/components/Hero';
import MemberJourney from '@/components/MemberJourney';
import ForMembers from '@/components/ForMembers';
import ForStudioTeams from '@/components/ForStudioTeams';
import Footer from '@/components/Footer';

const classes = [
  { id: 1, name: 'HIIT & Strength', time: '06:00', trainer: 'Coach Sarah', spots: 14, type: 'HIIT' },
  { id: 2, name: 'Yoga Flow', time: '07:30', trainer: 'Coach Emma', spots: 8, type: 'Yoga' },
  { id: 3, name: 'Cycling Power', time: '09:00', trainer: 'Coach Marcus', spots: 3, type: 'Cycling' },
  { id: 4, name: 'Box & Burn', time: '17:30', trainer: 'Coach Leo', spots: 12, type: 'Boxing' },
  { id: 5, name: 'Pilates Core', time: '18:30', trainer: 'Coach Nina', spots: 6, type: 'Pilates' },
  { id: 6, name: 'Evening Run Club', time: '19:00', trainer: 'Coach Dan', spots: 20, type: 'Running' },
];

const coaches = [
  { name: 'Sarah Mitchell', role: 'Head of HIIT & Conditioning', years: 8 },
  { name: 'Marcus Chen', role: 'Cycling & Endurance Coach', years: 5 },
  { name: 'Emma Walsh', role: 'Yoga & Mobility Specialist', years: 6 },
  { name: 'Leo Torres', role: 'Boxing & Combat Fitness', years: 4 },
];

const plans = [
  {
    name: 'Move',
    price: '£29',
    period: '/month',
    desc: 'Perfect for getting started',
    features: ['Up to 8 classes/month', 'App access', 'Progress tracking', 'Community access'],
    highlight: false,
  },
  {
    name: 'Perform',
    price: '£49',
    period: '/month',
    desc: 'Our most popular plan',
    features: ['Unlimited classes', 'AI coaching', 'Priority booking', 'Nutrition guidance', 'Monthly check-in'],
    highlight: true,
  },
  {
    name: 'PT Focus',
    price: '£89',
    period: '/month',
    desc: 'Maximum results with personal training',
    features: ['Unlimited classes', '4 PT sessions/month', 'Custom programme', 'Body composition tracking', 'Direct trainer access'],
    highlight: false,
  },
];

const faqs = [
  { q: 'Can I freeze my membership?', a: 'Yes — you can freeze for up to 3 months per year at no cost.' },
  { q: 'Is there a joining fee?', a: 'No joining fee ever. Just your monthly subscription.' },
  { q: 'Can I try before I buy?', a: 'Yes! New members get their first class completely free.' },
  { q: 'How do I cancel?', a: 'Cancel any time with 30 days notice directly in the app — no phone calls needed.' },
  { q: 'What happens if I miss a class?', a: 'Cancel at least 2 hours before to free your spot for another member.' },
  { q: 'Are there family discounts?', a: 'Yes — add a family member for 20% off their membership.' },
];

export default function LandingPage() {
  return (
    <>
      <Header />
      <main className="pt-16">
        <Hero />

        {/* Trust strip */}
        <section className="bg-[#ECEDE6] py-6 border-y border-[#DADCD3]">
          <div className="max-w-[1280px] mx-auto px-10 flex flex-wrap items-center justify-center gap-10">
            {[
              { value: '2,400+', label: 'Active members' },
              { value: '40+', label: 'Weekly classes' },
              { value: '15+', label: 'Expert coaches' },
              { value: '98%', label: 'Member satisfaction' },
            ].map((m) => (
              <div key={m.label} className="text-center">
                <p className="text-xl font-bold text-[#111310]">{m.value}</p>
                <p className="text-xs text-[#5E625A] mt-0.5">{m.label}</p>
              </div>
            ))}
          </div>
        </section>

        <MemberJourney />
        <ForMembers />
        <ForStudioTeams />

        {/* Classes Today */}
        <section className="bg-white py-20">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">TODAY&apos;S CLASSES</p>
            <h2 className="text-[38px] font-bold text-[#111310] mb-4">What&apos;s on today</h2>
            <p className="text-base text-[#5E625A] max-w-xl mb-12">Book your spot in any class — reserve online and walk in ready to train.</p>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {classes.map((c) => (
                <div key={c.id} className="bg-white border border-[#DADCD3] rounded-xl p-5">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#526B00] bg-[#F5F5F0] px-2 py-1 rounded-full">{c.type}</span>
                    <span className="text-xs text-[#5E625A]">{c.time}</span>
                  </div>
                  <h3 className="text-base font-bold text-[#111310] mb-1">{c.name}</h3>
                  <p className="text-sm text-[#5E625A] mb-4">{c.trainer}</p>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#5E625A]">{c.spots} spots left</span>
                    <a href="/register" className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-1.5 text-xs hover:bg-[#C4E82E] transition-colors">Book</a>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Coaches */}
        <section className="bg-[#F5F5F0] py-20">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">OUR COACHES</p>
            <h2 className="text-[38px] font-bold text-[#111310] mb-4">World-class coaches</h2>
            <p className="text-base text-[#5E625A] max-w-xl mb-12">Every coach is certified, experienced, and passionate about your results.</p>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              {coaches.map((c) => (
                <div key={c.name} className="bg-white border border-[#DADCD3] rounded-xl p-5 text-center">
                  <div className="w-16 h-16 rounded-full bg-[#D7FF3F] flex items-center justify-center mx-auto mb-3">
                    <span className="text-lg font-bold text-[#111310]">{c.name.split(' ').map(n => n[0]).join('')}</span>
                  </div>
                  <h3 className="text-sm font-bold text-[#111310] mb-1">{c.name}</h3>
                  <p className="text-xs text-[#5E625A] mb-2">{c.role}</p>
                  <p className="text-[10px] text-[#5E625A]">{c.years} years experience</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Pricing */}
        <section className="bg-white py-20">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">PRICING</p>
            <h2 className="text-[38px] font-bold text-[#111310] mb-4">Simple, honest pricing</h2>
            <p className="text-base text-[#5E625A] max-w-xl mb-12">No joining fee. No hidden costs. Cancel any time.</p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl">
              {plans.map((p) => (
                <div key={p.name} className={`rounded-2xl p-7 border ${
                  p.highlight
                    ? 'bg-[#171A16] border-[#D7FF3F]'
                    : 'bg-white border-[#DADCD3]'
                }`}>
                  <h3 className={`text-lg font-bold mb-1 ${ p.highlight ? 'text-white' : 'text-[#111310]' }`}>{p.name}</h3>
                  <p className={`text-xs mb-4 ${ p.highlight ? 'text-[#A8ADA2]' : 'text-[#5E625A]' }`}>{p.desc}</p>
                  <div className="flex items-baseline gap-1 mb-6">
                    <span className={`text-3xl font-bold ${ p.highlight ? 'text-[#D7FF3F]' : 'text-[#111310]' }`}>{p.price}</span>
                    <span className={`text-sm ${ p.highlight ? 'text-[#A8ADA2]' : 'text-[#5E625A]' }`}>{p.period}</span>
                  </div>
                  <ul className="space-y-2 mb-7">
                    {p.features.map((f) => (
                      <li key={f} className={`flex items-start gap-2 text-sm ${ p.highlight ? 'text-[#ECEDE6]' : 'text-[#5E625A]' }`}>
                        <span className={`mt-0.5 ${ p.highlight ? 'text-[#D7FF3F]' : 'text-[#526B00]' }`}>✓</span>
                        {f}
                      </li>
                    ))}
                  </ul>
                  <a href="/register" className={`block text-center font-semibold rounded-lg px-5 py-2.5 text-sm ${
                    p.highlight
                      ? 'bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E]'
                      : 'bg-[#171A16] text-[#ECEDE6] hover:bg-black/80'
                  } transition-colors`}>
                    Get started
                  </a>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Testimonials */}
        <section className="bg-[#F5F5F0] py-20">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">TESTIMONIALS</p>
            <h2 className="text-[38px] font-bold text-[#111310] mb-12">What members say</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                { name: 'Alex P.', quote: 'FITOS completely changed how I train. The AI coaching alone is worth it.', plan: 'Perform member' },
                { name: 'Priya S.', quote: 'Booking classes is so easy and the coaches are genuinely world-class.', plan: 'PT Focus member' },
                { name: 'Tom K.', quote: 'As a studio owner the CRM has tripled our lead conversion. Game changer.', plan: 'Studio owner' },
              ].map((t) => (
                <div key={t.name} className="bg-white border border-[#DADCD3] rounded-xl p-6">
                  <p className="text-sm text-[#5E625A] mb-5 leading-relaxed">&quot;{t.quote}&quot;</p>
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-[#D7FF3F] flex items-center justify-center">
                      <span className="text-xs font-bold text-[#111310]">{t.name[0]}</span>
                    </div>
                    <div>
                      <p className="text-sm font-bold text-[#111310]">{t.name}</p>
                      <p className="text-xs text-[#5E625A]">{t.plan}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* FAQ */}
        <section className="bg-white py-20">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">FAQ</p>
            <h2 className="text-[38px] font-bold text-[#111310] mb-12">Common questions</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-3xl">
              {faqs.map((f) => (
                <div key={f.q} className="bg-[#F5F5F0] rounded-xl p-5">
                  <h3 className="text-sm font-bold text-[#111310] mb-2">{f.q}</h3>
                  <p className="text-sm text-[#5E625A]">{f.a}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Dark CTA */}
        <section className="bg-[#171A16] py-20">
          <div className="max-w-[1280px] mx-auto px-10 text-center">
            <h2 className="text-[38px] font-bold text-white mb-4">Ready to start training?</h2>
            <p className="text-base text-[#A8ADA2] max-w-md mx-auto mb-8">Join thousands of members already training smarter with FITOS.</p>
            <div className="flex items-center justify-center gap-4">
              <a href="/register" className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-6 py-3 text-sm hover:bg-[#C4E82E] transition-colors">Start free trial</a>
              <a href="/plans" className="border border-white/20 text-[#ECEDE6] font-semibold rounded-lg px-6 py-3 text-sm hover:bg-white/5 transition-colors">View plans</a>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
