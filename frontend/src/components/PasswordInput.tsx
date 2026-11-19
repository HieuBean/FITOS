'use client';

import { useState, type InputHTMLAttributes } from 'react';

type PasswordInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>;

export default function PasswordInput({ className = '', disabled, id, ...props }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <input
        {...props}
        id={id}
        type={visible ? 'text' : 'password'}
        disabled={disabled}
        className={`${className} pr-12`}
      />
      <button
        type="button"
        aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
        aria-pressed={visible}
        aria-controls={id}
        disabled={disabled}
        onClick={() => setVisible((current) => !current)}
        className="absolute inset-y-0 right-0 flex w-11 items-center justify-center rounded-r-lg text-[#5E625A] hover:text-[#111310] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#526B00] disabled:opacity-60"
      >
        {visible ? (
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
            <path d="M3 3l18 18" />
            <path d="M10.6 5.1A10.9 10.9 0 0 1 12 5c5 0 9.3 3 11 7-0.7 1.6-1.8 3-3.1 4.1" />
            <path d="M6.5 6.5C4.1 7.7 2.3 9.7 1 12c1.7 4 6 7 11 7 1.2 0 2.4-0.2 3.5-0.6" />
            <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
          </svg>
        ) : (
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
            <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        )}
      </button>
    </div>
  );
}
