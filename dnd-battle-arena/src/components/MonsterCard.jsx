import React from 'react'

const MonsterCard = ({ monster }) => {
    if (!monster) return null;

    const healthPercentage = (monster.hp / monster.maxHp) * 100;
    let healthColor = 'bg-green-500';
    if (healthPercentage < 60) healthColor = 'bg-yellow-500';
    if (healthPercentage < 30) healthColor = 'bg-red-600';

    return (
        <div className="card w-full max-w-sm text-center">
            <h2 className="text-3xl font-bold title-font text-red-500 mb-4">{monster.name}</h2>
            <div className="relative">
                <img src={monster.imageUrl} alt={monster.name} className="h-80 object-contain mb-4 rounded-lg shadow-lg" />
                <div className="absolute bottom-4 left-0 right-0 flex justify-center">
                    <p className="bg-black bg-opacity-75 text-white text-xl font-bold px-4 py-2 rounded-full">
                        {monster.hp} / {monster.maxHp} HP
                    </p>
                </div>
            </div>
            <div className="health-bar-bg">
                <div className={`health-bar ${healthColor}`} style={{ width: `${healthPercentage}%` }}></div>
            </div>
        </div>
    );
};

export default MonsterCard