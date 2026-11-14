import Image from 'next/image';

export default function Hero() {
  return (
    <section className="bg-[#F5F5F0] py-20">
      <div className="max-w-[1280px] mx-auto px-10 flex items-center gap-12">
        {/* Left column */}
        <div className="flex-1">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 bg-white border border-[#DADCD3] rounded-full px-4 py-1.5 mb-6">
            <span className="w-2 h-2 rounded-full bg-[#D7FF3F]"></span>
            <span className="text-xs font-semibold text-[#111310]">Now open — City Centre Studio</span>
          </div>
          {/* Heading */}
          <h1 className="text-[52px] font-bold text-[#111310] leading-[1.1] mb-5">
            Train smarter.<br />
            <span className="text-[#526B00]">Move better.</span>
          </h1>
          {/* Subtitle */}
          <p className="text-lg text-[#5E625A] max-w-md mb-8">
            FITOS connects members, trainers, and studio teams in one platform — so everyone can focus on what matters most: great training.
          </p>
          {/* Buttons */}
          <div className="flex items-center gap-4 mb-12">
            <a href="/register" className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-6 py-3 text-sm hover:bg-[#C4E82E] transition-colors">Start free trial</a>
            <a href="/plans" className="bg-[#171A16] text-[#ECEDE6] font-semibold rounded-lg px-6 py-3 text-sm hover:bg-black/80 transition-colors">View plans</a>
          </div>
          {/* Stats */}
          <div className="flex items-center gap-8">
            <div>
              <p className="text-2xl font-bold text-[#111310]">2,400+</p>
              <p className="text-xs text-[#5E625A] mt-0.5">Active members</p>
            </div>
            <div className="w-px h-8 bg-[#DADCD3]"></div>
            <div>
              <p className="text-2xl font-bold text-[#111310]">98%</p>
              <p className="text-xs text-[#5E625A] mt-0.5">Satisfaction rate</p>
            </div>
            <div className="w-px h-8 bg-[#DADCD3]"></div>
            <div>
              <p className="text-2xl font-bold text-[#111310]">40+</p>
              <p className="text-xs text-[#5E625A] mt-0.5">Classes weekly</p>
            </div>
          </div>
        </div>
        {/* Right column — image */}
        <div className="hidden lg:block w-[500px] flex-shrink-0">
          <div className="relative h-[460px] rounded-2xl overflow-hidden">
            <Image
              src="/assets/hero_athlete.jpg"
              alt="Athlete training at FITOS"
              fill
              className="object-cover"
              priority
            />
            {/* Overlay card */}
            <div className="absolute bottom-5 right-5 bg-white/90 backdrop-blur-sm border border-[#DADCD3] rounded-xl p-4 w-48">
              <p className="text-xs text-[#526B00] font-semibold mb-1">TODAY&apos;S CLASS</p>
              <p className="text-sm font-bold text-[#111310]">HIIT &amp; Strength</p>
              <p className="text-xs text-[#5E625A] mt-0.5">6:00 AM · 14 spots left</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
